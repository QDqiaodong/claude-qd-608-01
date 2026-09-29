package com.garment.factory.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garment.factory.common.BizException;
import com.garment.factory.domain.GradeDict;
import com.garment.factory.domain.GradeScheme;
import com.garment.factory.domain.Style;
import com.garment.factory.dto.GradeCompareResult;
import com.garment.factory.dto.GradeResult;
import com.garment.factory.dto.GradeSchemeDetailDto;
import com.garment.factory.dto.GradeSchemeListItemDto;
import com.garment.factory.dto.GradeSchemeReq;
import com.garment.factory.repository.GradeSchemeRepository;
import com.garment.factory.repository.StyleRepository;
import com.garment.factory.service.grade.CanonicalScheme;
import com.garment.factory.service.grade.GradeCanonicalCodec;
import com.garment.factory.service.grade.GradeComparator;
import com.garment.factory.service.grade.GradeEngine;
import com.garment.factory.service.grade.GradeNormalizer;

/**
 * 放码方案服务：计算规则、精度口径与完整结果全部由服务端统一保证。
 *
 * 原子性口径：
 *   试算/留存都先在内存中把【整组结果】算完（含全部尺码×部位×侧别），
 *   任何公式无法计算（normalizer/engine 抛错）都不会触碰已落库快照 ——
 *   数据库里永远只有「上一份完整结果」，不存在半套新结果。
 * 重开口径：
 *   详情直接反序列化 snapshot_json 原样返回，不重算、不重舍入；
 *   旧厘米数据（legacy_flag=1）按 0.1mm 步进兼容读入，结果仍是旧的两位小数厘米值。
 */
@Service
public class GradeSchemeService {

    private final GradeSchemeRepository repository;
    private final StyleRepository styleRepository;
    private final GradeNormalizer normalizer;
    private final GradeEngine engine;
    private final GradeComparator comparator;
    private final GradeCanonicalCodec codec;
    private final ObjectMapper mapper = new ObjectMapper();

    public GradeSchemeService(GradeSchemeRepository repository, StyleRepository styleRepository,
                              GradeNormalizer normalizer, GradeEngine engine,
                              GradeComparator comparator, GradeCanonicalCodec codec) {
        this.repository = repository;
        this.styleRepository = styleRepository;
        this.normalizer = normalizer;
        this.engine = engine;
        this.comparator = comparator;
        this.codec = codec;
    }

    /** 临时试算：不落库、不污染任何已有方案，整组结果一次性返回。 */
    public GradeResult preview(GradeSchemeReq req) {
        CanonicalScheme scheme = normalizer.normalize(req);
        return engine.compute(scheme);
    }

    @Transactional(readOnly = true)
    public List<GradeSchemeListItemDto> list() {
        List<GradeScheme> all = repository.findAllByOrderByUpdatedAtDesc();
        List<GradeSchemeListItemDto> result = new ArrayList<>();
        for (GradeScheme s : all) {
            GradeResult snapshot = readSnapshot(s);
            String styleName = s.getStyleId() == null ? "临时计算方案"
                    : styleRepository.findById(s.getStyleId()).map(Style::getName).orElse("未绑定款式");
            result.add(new GradeSchemeListItemDto(s.getId(), s.getSchemeNo(), s.getName(), s.getStyleId(),
                    styleName, s.getBaseSizeCode(), s.getInputUnit(),
                    snapshot.getSizes().size(), snapshot.getParts().size(), snapshot.getIssues().size(),
                    s.getLegacyFlag(), s.getResultVersion(), s.getUpdatedAt()));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public GradeSchemeDetailDto detail(Long id) {
        GradeScheme entity = require(id);
        return toDetail(entity);
    }

    /**
     * 留存（新建或改版）：先整组算完，成功后才落库，旧快照在同一事务内被整体替换。
     */
    @Transactional
    public GradeSchemeDetailDto save(GradeSchemeReq req) {
        validateHeader(req);
        // 先算：失败则直接抛出，下面的持久化代码完全不执行，旧快照保持不变
        CanonicalScheme canonical = normalizer.normalize(req);
        GradeResult result = engine.compute(canonical);

        GradeScheme entity = req.getId() == null ? new GradeScheme() : require(req.getId());
        if (req.getId() == null && repository.existsBySchemeNo(req.getSchemeNo())) {
            throw new BizException("放码方案号已存在：" + req.getSchemeNo());
        }
        if (req.getId() != null && repository.existsBySchemeNoAndIdNot(req.getSchemeNo(), req.getId())) {
            throw new BizException("方案号已被其它方案占用：" + req.getSchemeNo());
        }

        entity.setSchemeNo(req.getSchemeNo());
        entity.setName(req.getName());
        entity.setStyleId(req.getStyleId());
        entity.setStatus("SAVED");
        entity.setBaseSizeCode(canonical.getBaseSizeCode());
        entity.setInputUnit(canonical.getInputUnit());
        entity.setRoundStepMm(canonical.getRoundStepMm());
        entity.setSizesJson(codec.writeSizes(canonical.getSizes()));
        entity.setBasesJson(codec.writeBases(canonical.getBases()));
        entity.setRulesJson(codec.writeRules(canonical.getRules()));
        entity.setSnapshotJson(writeSnapshot(result));
        // 兼容标记只属于历史迁移数据，页面新建/改版一律不是旧数据，忽略请求中的该字段
        entity.setLegacyFlag(false);
        entity.setResultVersion(entity.getResultVersion() == null ? 1 : entity.getResultVersion() + 1);
        LocalDateTime now = LocalDateTime.now();
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);
        GradeScheme saved = repository.save(entity);
        return toDetail(saved);
    }

    @Transactional
    public void remove(Long id) {
        repository.delete(require(id));
    }

    /** 两方案逐部位比较：公式差异 / 舍入差异 / 最终尺寸差异分开统计。 */
    @Transactional(readOnly = true)
    public GradeCompareResult compare(Long idA, Long idB) {
        if (idA.equals(idB)) {
            throw new BizException("请选择两个不同的放码方案进行比较");
        }
        GradeScheme a = require(idA);
        GradeScheme b = require(idB);
        GradeResult ra = readSnapshot(a);
        GradeResult rb = readSnapshot(b);
        CanonicalScheme ca = toCanonical(a);
        CanonicalScheme cb = toCanonical(b);
        return comparator.compare(ra, rb, ca, cb, a.getId(), a.getName(), b.getId(), b.getName());
    }

    // ------------------------------------------------------------------ 内部

    private GradeScheme require(Long id) {
        return repository.findById(id).orElseThrow(() -> new BizException("放码方案不存在"));
    }

    private void validateHeader(GradeSchemeReq req) {
        if (req.getSchemeNo() == null || req.getSchemeNo().isBlank()) {
            throw new BizException("方案编号不能为空");
        }
        if (req.getName() == null || req.getName().isBlank()) {
            throw new BizException("方案名称不能为空");
        }
        if (req.getStyleId() != null && styleRepository.findById(req.getStyleId()).isEmpty()) {
            throw new BizException("关联款式不存在");
        }
    }

    private GradeResult readSnapshot(GradeScheme entity) {
        try {
            return mapper.readValue(entity.getSnapshotJson(), GradeResult.class);
        } catch (Exception e) {
            throw new BizException("方案「" + entity.getSchemeNo() + "」的结果快照已损坏，无法读取");
        }
    }

    private String writeSnapshot(GradeResult result) {
        try {
            return mapper.writeValueAsString(result);
        } catch (Exception e) {
            throw new BizException("结果快照写入失败：" + e.getMessage());
        }
    }

    /** 用三列毫米规范数据重建规范化方案（比较时取规则签名用）。 */
    private CanonicalScheme toCanonical(GradeScheme entity) {
        CanonicalScheme scheme = new CanonicalScheme();
        scheme.setInputUnit(entity.getInputUnit());
        scheme.setRoundStepMm(entity.getRoundStepMm());
        scheme.setSizes(codec.readSizes(entity.getSizesJson()));
        List<Integer> levels = new ArrayList<>();
        int baseIndex = scheme.getSizes().indexOf(entity.getBaseSizeCode());
        for (int i = 0; i < scheme.getSizes().size(); i++) {
            levels.add(i - baseIndex);
        }
        scheme.setLevels(levels);
        scheme.setBaseSizeCode(entity.getBaseSizeCode());
        scheme.setBaseIndex(baseIndex);
        scheme.setBases(codec.readBases(entity.getBasesJson()));
        scheme.setRules(codec.readRules(entity.getRulesJson()));
        scheme.setLegacy(Boolean.TRUE.equals(entity.getLegacyFlag()));
        return scheme;
    }

    private GradeSchemeDetailDto toDetail(GradeScheme entity) {
        CanonicalScheme canonical = toCanonical(entity);
        GradeSchemeDetailDto dto = new GradeSchemeDetailDto();
        dto.setId(entity.getId());
        dto.setSchemeNo(entity.getSchemeNo());
        dto.setName(entity.getName());
        dto.setStyleId(entity.getStyleId());
        dto.setInputUnit(entity.getInputUnit());
        dto.setRoundStepMm(entity.getRoundStepMm());
        dto.setBaseSizeCode(entity.getBaseSizeCode());
        dto.setSizes(canonical.getSizes());
        // 回显值按当初录入单位给出；切换单位只在页面上做显示换算，不触发任何重算
        dto.setBases(codec.toBaseReqs(canonical.getBases(), entity.getInputUnit()));
        dto.setRules(codec.toRuleReqs(canonical.getRules(), entity.getInputUnit()));
        dto.setLegacyFlag(entity.getLegacyFlag());
        dto.setResultVersion(entity.getResultVersion());
        dto.setResult(readSnapshot(entity));
        return dto;
    }

    /** 部位默认元数据（给前端初始化录入表单用） */
    public java.util.Map<String, Object> meta() {
        java.util.Map<String, Object> meta = new java.util.LinkedHashMap<>();
        meta.put("parts", GradeDict.parts());
        meta.put("partKeys", GradeDict.partKeys());
        meta.put("symmetricParts", GradeDict.partKeys().stream().filter(GradeDict::partSymmetric).toList());
        meta.put("ruleTypes", GradeDict.ruleTypes());
        meta.put("sides", GradeDict.sides());
        meta.put("units", GradeDict.units());
        meta.put("defaultRoundStepMm", GradeDict.DEFAULT_ROUND_STEP_MM);
        meta.put("shoulderRatioMin", GradeDict.SHOULDER_RATIO_MIN);
        meta.put("shoulderRatioMax", GradeDict.SHOULDER_RATIO_MAX);
        return meta;
    }
}
