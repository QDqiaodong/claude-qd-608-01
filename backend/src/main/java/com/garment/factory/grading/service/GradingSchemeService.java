package com.garment.factory.grading.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garment.factory.common.BizException;
import com.garment.factory.grading.doc.LegacySchemeMigrator;
import com.garment.factory.grading.doc.ResultDoc;
import com.garment.factory.grading.doc.SchemeDoc;
import com.garment.factory.grading.doc.SchemeDocMapper;
import com.garment.factory.grading.engine.DecimalQuantity;
import com.garment.factory.grading.engine.GradingAnomaly;
import com.garment.factory.grading.engine.GradingCell;
import com.garment.factory.grading.engine.GradingGenerator;
import com.garment.factory.grading.engine.GradingPart;
import com.garment.factory.grading.engine.GradingResultSet;
import com.garment.factory.grading.engine.GradingSpec;
import com.garment.factory.grading.engine.LengthUnit;
import com.garment.factory.grading.engine.SchemeComparator;
import com.garment.factory.grading.model.GradingScheme;
import com.garment.factory.grading.repo.GradingSchemeRepository;

/**
 * 放码方案应用服务。
 *
 * <p>原子性保证（验收硬口径）：{@link #generate} 先在内存完成 <b>解析 → 计算 → 异常检测</b> 全程，
 * 任何公式/结构错误在此阶段抛 {@link BizException}，事务回滚，{@code resultJson} 不被触碰，
 * 版师看到的仍是上一次完整结果；只有整套结果成功产出才一次性落库。
 */
@Service
public class GradingSchemeService {

    private final GradingSchemeRepository repository;
    private final SchemeDocMapper mapper;
    private final ObjectMapper objectMapper;

    public GradingSchemeService(GradingSchemeRepository repository,
                                SchemeDocMapper mapper,
                                ObjectMapper objectMapper) {
        this.repository = repository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    /* ============================================================== 查询 / 留存 */

    @Transactional(readOnly = true)
    public Map<String, Object> list(Boolean temporary, String keyword, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)),
                Sort.by(Sort.Direction.DESC, "updatedAt"));
        String kw = keyword == null ? "" : keyword.trim();
        Page<GradingScheme> result = kw.isEmpty()
                ? repository.findByTemporary(Boolean.TRUE.equals(temporary), pageable)
                : repository.findByTemporaryAndNameContaining(Boolean.TRUE.equals(temporary), kw, pageable);

        List<Map<String, Object>> items = new ArrayList<>();
        for (GradingScheme s : result.getContent()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", s.getId());
            item.put("schemeNo", s.getSchemeNo());
            item.put("name", s.getName());
            item.put("styleId", s.getStyleId());
            item.put("temporary", s.getTemporary());
            item.put("schemaVersion", s.getSchemaVersion());
            item.put("hasResult", s.getResultJson() != null && !s.getResultJson().isBlank());
            item.put("resultGeneratedAt", s.getResultGeneratedAt());
            item.put("updatedAt", s.getUpdatedAt());
            item.put("remark", s.getRemark());
            items.add(item);
        }
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("content", items);
        resp.put("page", result.getNumber());
        resp.put("size", result.getSize());
        resp.put("total", result.getTotalElements());
        resp.put("hasNext", result.hasNext());
        return resp;
    }

    /**
     * 读取方案详情 + 上一次结果。v1 旧数据在此刻兼容迁移（只影响返回与后续生成，不改动库存 JSON，
     * 除非显式保存）。
     */
    @Transactional(readOnly = true)
    public Map<String, Object> detail(Long id) {
        GradingScheme entity = require(id);
        ParsedScheme parsed = parseEntity(entity);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("id", entity.getId());
        resp.put("schemeNo", entity.getSchemeNo());
        resp.put("name", entity.getName());
        resp.put("styleId", entity.getStyleId());
        resp.put("temporary", entity.getTemporary());
        resp.put("storedSchemaVersion", entity.getSchemaVersion());
        resp.put("remark", entity.getRemark());
        resp.put("createdAt", entity.getCreatedAt());
        resp.put("updatedAt", entity.getUpdatedAt());
        resp.put("spec", parsed.doc());
        resp.put("contentHash", entity.getContentHash());
        resp.put("resultGeneratedAt", entity.getResultGeneratedAt());
        resp.put("result", readStoredResult(entity));
        return resp;
    }

    /** 新建或更新方案（留存）。临时方案同样落库，避免刷新丢失；temporary=1 仅做分组标识。 */
    @Transactional
    public GradingScheme save(Long id, String schemeNo, String name, Long styleId, boolean temporary,
                              String remark, SchemeDoc doc) {
        if (name == null || name.isBlank()) {
            throw new BizException("方案名称不能为空");
        }
        // 先完整解析 + 试算一遍：结构不合法的方案不允许留存
        GradingSpec spec = mapper.toSpec(doc);
        GradingGenerator.generate(spec);
        String newHash = sha256(normalize(doc));

        GradingScheme entity = id == null ? new GradingScheme() : require(id);
        if (id == null) {
            entity.setCreatedAt(LocalDateTime.now());
            String no = (schemeNo == null || schemeNo.isBlank()) ? nextSchemeNo() : schemeNo.trim();
            if (repository.existsBySchemeNo(no)) {
                throw new BizException("方案编号已存在：" + no);
            }
            entity.setSchemeNo(no);
        }
        entity.setName(name.trim());
        entity.setStyleId(styleId);
        entity.setTemporary(temporary);
        entity.setRemark(remark);
        entity.setSchemaVersion(2);
        entity.setSpecJson(writeJson(doc));
        entity.setContentHash(newHash);
        entity.setUpdatedAt(LocalDateTime.now());
        // 关于「不残留上一次结果」：这里不直接删旧结果，而是靠 contentHash 让 detail/readStoredResult
        // 把旧结果标记 stale 并在前端明确提示「参数已改，需重新生成」。这样既不会让旧数值冒充新方案结果，
        // 又满足“公式无法计算时整次生成失败、上一次完整结果仍保留供核对”的原子性口径。
        return repository.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(require(id));
    }

    /* ============================================================== 生成（原子） */

    /**
     * 依据已留存方案重新生成整组结果。成功才覆盖；失败保留上一次完整结果。
     */
    @Transactional
    public ResultDoc generate(Long id) {
        GradingScheme entity = require(id);
        ParsedScheme parsed = parseEntity(entity);
        GradingResultSet rs = computeSafely(parsed.spec());

        ResultDoc resultDoc = toResultDoc(rs, parsed.doc(), entity.getContentHash());
        // —— 全部成功后才写结果字段 ——
        entity.setResultJson(writeJson(resultDoc));
        entity.setResultGeneratedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        repository.save(entity);
        return resultDoc;
    }

    /**
     * 临时计算：不落库、不影响任何既有方案。每次入参独立计算，绝不复用上一次对象。
     */
    @Transactional(readOnly = true)
    public Map<String, Object> preview(SchemeDoc doc) {
        GradingSpec spec = mapper.toSpec(doc);
        GradingResultSet rs = computeSafely(spec);
        String hash = sha256(normalize(doc));
        ResultDoc resultDoc = toResultDoc(rs, doc, hash);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("contentHash", hash);
        resp.put("result", resultDoc);
        return resp;
    }

    /**
     * 保存方案的同时立即生成（保存成功 + 整套结果成功才算成功；任一失败整体回滚，旧结果保留）。
     */
    @Transactional
    public Map<String, Object> saveAndGenerate(Long id, String schemeNo, String name, Long styleId,
                                               boolean temporary, String remark, SchemeDoc doc) {
        GradingSpec spec = mapper.toSpec(doc);
        GradingResultSet rs = computeSafely(spec); // 先算，失败则整事务回滚，绝不留下半套

        GradingScheme entity = id == null ? new GradingScheme() : require(id);
        LocalDateTime now = LocalDateTime.now();
        if (id == null) {
            entity.setCreatedAt(now);
            String no = (schemeNo == null || schemeNo.isBlank()) ? nextSchemeNo() : schemeNo.trim();
            if (repository.existsBySchemeNo(no)) {
                throw new BizException("方案编号已存在：" + no);
            }
            entity.setSchemeNo(no);
        }
        entity.setName(name == null || name.isBlank() ? "未命名放码方案" : name.trim());
        entity.setStyleId(styleId);
        entity.setTemporary(temporary);
        entity.setRemark(remark);
        entity.setSchemaVersion(2);
        String specJson = writeJson(doc);
        entity.setSpecJson(specJson);
        String hash = sha256(normalize(doc));
        entity.setContentHash(hash);
        entity.setUpdatedAt(now);

        ResultDoc resultDoc = toResultDoc(rs, doc, hash);
        entity.setResultJson(writeJson(resultDoc));
        entity.setResultGeneratedAt(now);
        repository.save(entity);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("id", entity.getId());
        resp.put("schemeNo", entity.getSchemeNo());
        resp.put("contentHash", hash);
        resp.put("result", resultDoc);
        return resp;
    }

    /**
     * 计算并把任何引擎异常统一转成业务异常（保证不留半成品）。
     */
    private GradingResultSet computeSafely(GradingSpec spec) {
        long start = System.nanoTime();
        try {
            GradingResultSet rs = GradingGenerator.generate(spec);
            long elapsedMicros = (System.nanoTime() - start) / 1_000;
            rs.elapsedMicrosForDisplay = elapsedMicros;
            return rs;
        } catch (BizException e) {
            throw e;
        } catch (ArithmeticException e) {
            throw new BizException("放码公式无法计算：" + e.getMessage());
        }
    }

    /* ============================================================== 两方案比较 */

    @Transactional(readOnly = true)
    public Map<String, Object> compare(Long schemeAId, Long schemeBId) {
        GradingScheme a = require(schemeAId);
        GradingScheme b = require(schemeBId);
        GradingResultSet ra = computeSafely(parseEntity(a).spec());
        GradingResultSet rb = computeSafely(parseEntity(b).spec());
        SchemeComparator.CompareResult cmp = SchemeComparator.compare(ra, rb);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("schemeA", schemeBrief(a));
        resp.put("schemeB", schemeBrief(b));
        resp.put("counts", cmp.counts());
        resp.put("commonSizeCount", cmp.commonSizeCount());

        List<Map<String, Object>> slots = new ArrayList<>();
        for (SchemeComparator.SlotDiff sd : cmp.slots()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("slotKey", sd.slotKey());
            m.put("nameA", sd.nameA());
            m.put("nameB", sd.nameB());
            m.put("onlyInA", sd.onlyInA());
            m.put("onlyInB", sd.onlyInB());
            m.put("formulaDifferent", sd.formulaDifferent());
            m.put("roundingDifferent", sd.roundingDifferent());
            m.put("finalDifferent", sd.finalDifferent());
            m.put("summary", sd.summary());
            List<Map<String, Object>> cells = new ArrayList<>();
            for (SchemeComparator.CellDiff cd : sd.cells()) {
                Map<String, Object> c = new LinkedHashMap<>();
                c.put("grade", cd.grade());
                c.put("sizeName", cd.sizeName());
                c.put("kind", cd.kind().name());
                c.put("finalACm", cm(cd.finalA()));
                c.put("finalBCm", cm(cd.finalB()));
                c.put("finalDeltaCm", signedCm(cd.finalDelta()));
                c.put("detail", cd.detail());
                cells.add(c);
            }
            m.put("cells", cells);
            slots.add(m);
        }
        resp.put("slots", slots);
        return resp;
    }

    private Map<String, Object> schemeBrief(GradingScheme s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("schemeNo", s.getSchemeNo());
        m.put("name", s.getName());
        return m;
    }

    /* ============================================================== 文档装配 */

    private ResultDoc toResultDoc(GradingResultSet rs, SchemeDoc doc, String hash) {
        ResultDoc out = new ResultDoc();
        out.setBaseGrade(rs.baseGrade());
        out.setRoundingUnit(doc.getRounding().getUnit());
        out.setRoundingStep(doc.getRounding().getStep());
        out.setQuantumMicrons(rs.quantumMicrons().stripTrailingZeros().toPlainString());

        List<ResultDoc.SizeView> sizeViews = new ArrayList<>();
        rs.sizes().forEach(s -> {
            ResultDoc.SizeView v = new ResultDoc.SizeView();
            v.setGrade(s.grade());
            v.setName(s.name());
            sizeViews.add(v);
        });
        out.setSizes(sizeViews);

        int cmDecimals = com.garment.factory.grading.engine.GradingRounding
                .displayDecimals(rs.quantumMicrons(), LengthUnit.CM);

        List<ResultDoc.SlotView> slotViews = new ArrayList<>();
        for (GradingPart part : rs.parts()) {
            ResultDoc.SlotView slot = new ResultDoc.SlotView();
            slot.setSlotKey(part.slotKey());
            slot.setPartId(part.partId());
            slot.setPartName(part.partName());
            slot.setSide(part.side());
            slot.setFormulaSignature(rs.formulaSignatures().get(part.slotKey()));

            List<ResultDoc.CellView> cellViews = new ArrayList<>();
            for (var size : rs.sizes()) {
                GradingCell cell = rs.cell(part.slotKey(), size.grade());
                ResultDoc.CellView cv = new ResultDoc.CellView();
                cv.setGrade(size.grade());
                cv.setSizeName(size.name());
                cv.setRawMicrons(cell.rawMicrons().toPlainString());
                cv.setFinalMicrons(cell.finalMicrons().toPlainString());
                cv.setBaseDeltaMicrons(cell.baseDeltaMicrons().toPlainString());
                // 单位换算在量子整数上精确整除，不产生新舍入
                cv.setCm(DecimalQuantity.toDisplay(cell.finalMicrons(), LengthUnit.CM, cmDecimals));
                cv.setMm(DecimalQuantity.toDisplay(cell.finalMicrons(), LengthUnit.MM,
                        com.garment.factory.grading.engine.GradingRounding
                                .displayDecimals(rs.quantumMicrons(), LengthUnit.MM)));
                cv.setDeltaCm(DecimalQuantity.toDisplay(cell.baseDeltaMicrons(), LengthUnit.CM, cmDecimals));
                cv.setDeltaMm(DecimalQuantity.toDisplay(cell.baseDeltaMicrons(), LengthUnit.MM,
                        com.garment.factory.grading.engine.GradingRounding
                                .displayDecimals(rs.quantumMicrons(), LengthUnit.MM)));
                cellViews.add(cv);
            }
            slot.setCells(cellViews);
            slotViews.add(slot);
        }
        out.setSlots(slotViews);

        List<ResultDoc.AnomalyView> anomalyViews = new ArrayList<>();
        for (GradingAnomaly a : rs.anomalies()) {
            ResultDoc.AnomalyView av = new ResultDoc.AnomalyView();
            av.setType(a.type().name());
            av.setTypeLabel(a.type().label());
            av.setGrade(a.grade());
            av.setSizeName(a.sizeName());
            av.setSlotKey(a.slotKey());
            av.setSlotName(a.slotName());
            av.setEdgeFromGrade(a.edgeFrom());
            av.setEdgeToGrade(a.edgeTo());
            av.setReason(a.reason());
            anomalyViews.add(av);
        }
        out.setAnomalies(anomalyViews);

        ResultDoc.Meta meta = new ResultDoc.Meta();
        meta.setSpecHash(hash);
        meta.setCellCount(rs.cells().size());
        meta.setElapsedMicros(rs.elapsedMicrosForDisplay);
        out.setMeta(meta);
        return out;
    }

    private ResultDoc readStoredResult(GradingScheme entity) {
        if (entity.getResultJson() == null || entity.getResultJson().isBlank()) {
            return null;
        }
        try {
            ResultDoc doc = objectMapper.readValue(entity.getResultJson(), ResultDoc.class);
            // 结果必须与当前方案内容匹配；方案改过则旧结果只作留档参考
            if (doc.getMeta() != null && entity.getContentHash() != null
                    && !entity.getContentHash().equals(doc.getMeta().getSpecHash())) {
                doc.setStale(true);
            }
            return doc;
        } catch (Exception e) {
            throw new BizException("已留存结果损坏，无法读取：" + e.getMessage());
        }
    }

    /* ============================================================== v1 兼容 */

    private ParsedScheme parseEntity(GradingScheme entity) {
        String raw = entity.getSpecJson();
        if (raw == null || raw.isBlank()) {
            throw new BizException("方案内容为空");
        }
        try {
            JsonNode root = objectMapper.readTree(raw);
            int version = root.path("schemaVersion").asInt(1);
            SchemeDoc doc;
            if (version < 2) {
                // 旧数据：只有厘米小数 → 按兼容规则读入
                doc = LegacySchemeMigrator.migrate(root, objectMapper);
            } else {
                doc = objectMapper.treeToValue(root, SchemeDoc.class);
                if (doc.getSchemaVersion() == null) {
                    doc.setSchemaVersion(2);
                }
            }
            return new ParsedScheme(doc, mapper.toSpec(doc));
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("方案 JSON 无法解析：" + e.getMessage());
        }
    }

    private record ParsedScheme(SchemeDoc doc, GradingSpec spec) {
    }

    /* ============================================================== 工具 */

    private GradingScheme require(Long id) {
        return repository.findById(id).orElseThrow(() -> new BizException("放码方案不存在：id=" + id));
    }

    private String nextSchemeNo() {
        long count = repository.count();
        String no;
        do {
            count++;
            no = String.format("GR-%05d", count);
        } while (repository.existsBySchemeNo(no));
        return no;
    }

    private String writeJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            throw new BizException("方案序列化失败：" + e.getMessage());
        }
    }

    /**
     * 规范化为稳定字节序列再哈希：递归按键名排序输出（数组顺序保留），
     * 消除 JSON 键序差异，保证同参数方案指纹一致、重开必得同值。
     */
    private String normalize(SchemeDoc doc) {
        try {
            JsonNode node = objectMapper.valueToTree(doc);
            Object sorted = sortJson(node);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(sorted);
        } catch (Exception e) {
            throw new BizException("方案规范化失败：" + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Object sortJson(JsonNode node) {
        if (node.isObject()) {
            java.util.TreeMap<String, Object> map = new java.util.TreeMap<>();
            node.fields().forEachRemaining(e -> map.put(e.getKey(), sortJson(e.getValue())));
            return map;
        }
        if (node.isArray()) {
            List<Object> list = new ArrayList<>();
            node.forEach(n -> list.add(sortJson(n)));
            return list;
        }
        // 值节点：统一走 convertValue，数字/字符串/布尔/空都能稳定映射
        return objectMapper.convertValue(node, Object.class);
    }

    private String sha256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new BizException("内容摘要计算失败：" + e.getMessage());
        }
    }

    private static String cm(BigDecimal microns) {
        if (microns == null) {
            return null;
        }
        return microns.divide(LengthUnit.CM.micronFactor()).stripTrailingZeros().toPlainString();
    }

    private static String signedCm(BigDecimal microns) {
        if (microns == null) {
            return null;
        }
        if (microns.signum() == 0) {
            return "0";
        }
        String v = cm(microns.abs());
        return microns.signum() < 0 ? "−" + v : "+" + v;
    }
}
