package com.garment.factory.grading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.garment.factory.common.BizException;
import com.garment.factory.grading.doc.ResultDoc;
import com.garment.factory.grading.doc.SchemeDoc;
import com.garment.factory.grading.model.GradingScheme;
import com.garment.factory.grading.repo.GradingSchemeRepository;
import com.garment.factory.grading.service.GradingSchemeService;

/**
 * 服务层验收（H2）：留存确定性、公式失败原子性、v1 旧数据兼容、cm/mm 往返、两方案比较。
 */
@SpringBootTest
@ActiveProfiles("test")
class GradingSchemeServiceTest {

    @Autowired
    private GradingSchemeService service;

    @Autowired
    private GradingSchemeRepository repository;

    private long saveStandard(String name, String unit) {
        SchemeDoc doc = TestSchemes.standard(unit, "cm", "0.1");
        Map<String, Object> resp = service.saveAndGenerate(null, null, name, null, false, null, doc);
        return ((Number) resp.get("id")).longValue();
    }

    @Test
    @DisplayName("留存后重开必得同值：两次生成的 result 完全一致（确定性）")
    void reopenGivesSameValues() {
        long id = saveStandard("确定性方案-cm", "cm");
        ResultDoc first = service.generate(id);
        ResultDoc second = service.generate(id);
        assertEquals(first.getQuantumMicrons(), second.getQuantumMicrons());
        assertEquals(first.getSlots().size(), second.getSlots().size());
        for (int i = 0; i < first.getSlots().size(); i++) {
            var a = first.getSlots().get(i);
            var b = second.getSlots().get(i);
            assertEquals(a.getFormulaSignature(), b.getFormulaSignature());
            for (int j = 0; j < a.getCells().size(); j++) {
                var ca = a.getCells().get(j);
                var cb = b.getCells().get(j);
                assertEquals(ca.getFinalMicrons(), cb.getFinalMicrons());
                assertEquals(ca.getRawMicrons(), cb.getRawMicrons());
                assertEquals(ca.getBaseDeltaMicrons(), cb.getBaseDeltaMicrons());
                assertEquals(ca.getCm(), cb.getCm());
                assertEquals(ca.getMm(), cb.getMm());
            }
        }
    }

    @Test
    @DisplayName("厘米方案与毫米方案结果同值：内部统一口径，换单位不漂移")
    void cmAndMmSchemesProduceSameResult() {
        long cmId = saveStandard("厘米方案", "cm");
        long mmId = saveStandard("毫米方案", "mm");
        ResultDoc cm = service.generate(cmId);
        ResultDoc mm = service.generate(mmId);
        assertEquals(cm.getSlots().size(), mm.getSlots().size());
        for (int i = 0; i < cm.getSlots().size(); i++) {
            for (int j = 0; j < cm.getSlots().get(i).getCells().size(); j++) {
                assertEquals(cm.getSlots().get(i).getCells().get(j).getFinalMicrons(),
                        mm.getSlots().get(i).getCells().get(j).getFinalMicrons());
            }
        }
    }

    @Test
    @DisplayName("公式无法计算时整次生成失败，上一次完整结果保留")
    void failedGenerationKeepsPreviousResult() {
        long id = saveStandard("原子性方案", "cm");
        GradingScheme before = repository.findById(id).orElseThrow();
        String previousResultJson = before.getResultJson();
        assertNotNull(previousResultJson);

        // 把腰围规则改成分段且故意留缺口（缺边 1→2）
        SchemeDoc broken = repository.findById(id).map(this::readSpec).orElseThrow();
        SchemeDoc.PartDoc waist = broken.getParts().stream()
                .filter(p -> "waist".equals(p.getPartId())).findFirst().orElseThrow();
        waist.getRule().setType("SEGMENT");
        waist.getRule().setDelta(null);
        waist.getRule().setSegments(List.of(
                segment(-2, 0, "3", "cm"),
                segment(0, 1, "5", "cm"))); // 缺 1→2
        // 直接落库坏 spec（绕过 save 的校验，模拟“改参数后点生成”）
        GradingScheme entity = repository.findById(id).orElseThrow();
        entity.setSpecJson(writeJson(broken));
        repository.save(entity);

        BizException ex = assertThrows(BizException.class, () -> service.generate(id));
        assertTrue(ex.getMessage().contains("缺少边 1→2"), ex.getMessage());

        // 上一次完整结果原样保留
        GradingScheme after = repository.findById(id).orElseThrow();
        assertEquals(previousResultJson, after.getResultJson(), "失败后旧结果必须原样保留");
        assertNotNull(after.getResultGeneratedAt());
    }

    @Test
    @DisplayName("saveAndGenerate 原子性：坏方案保存即失败，数据库不新增、无半套结果")
    void saveAndGenerateAtomic() {
        long beforeCount = repository.count();
        SchemeDoc broken = TestSchemes.standard("cm", "cm", "0.1");
        broken.getParts().get(0).setBase(null); // 基准缺失
        assertThrows(BizException.class,
                () -> service.saveAndGenerate(null, null, "坏方案", null, true, null, broken));
        assertEquals(beforeCount, repository.count());
    }

    @Test
    @DisplayName("改参数后旧结果标记 stale，不再冒充新方案结果")
    void staleResultAfterParamChange() {
        long id = saveStandard("改参数方案", "cm");
        SchemeDoc doc = repository.findById(id).map(this::readSpec).orElseThrow();
        // 仅留存改动（胸围增量 4 → 4.5），不立即生成
        service.save(id, null, "改参数方案", null, false, null, withBustDelta(doc, "4.5", "cm"));

        Map<String, Object> detail = service.detail(id);
        ResultDoc result = (ResultDoc) detail.get("result");
        assertNotNull(result, "旧结果仍保留供核对");
        assertTrue(result.isStale(), "参数改动后旧结果必须被标记为 stale");
    }

    @Test
    @DisplayName("v1 旧数据（只有厘米小数）兼容读入：按 0.1cm 口径，结果与旧放码表一致")
    void legacyV1ReadCompatibility() {
        // 直接构造一条 v1 记录
        String v1Json = """
                {
                  "baseSize":"M",
                  "sizes":[{"name":"S","grade":-1},{"name":"M","grade":0},{"name":"L","grade":1}],
                  "parts":[
                    {"partId":"bust","partName":"胸围","base":"90.0","delta":"4.0","direction":"UP"},
                    {"partId":"bodyLen","partName":"衣长","base":"62.0","delta":"1.5","direction":"UP"}
                  ]
                }
                """;
        GradingScheme legacy = new GradingScheme();
        legacy.setSchemeNo("GR-LEGACY-1");
        legacy.setName("旧版放码表");
        legacy.setTemporary(false);
        legacy.setSchemaVersion(1);
        legacy.setSpecJson(v1Json);
        legacy.setCreatedAt(java.time.LocalDateTime.now());
        legacy.setUpdatedAt(java.time.LocalDateTime.now());
        repository.save(legacy);

        // 生成不报错，数值按旧口径（cm 直接加减，保留 1 位）
        ResultDoc result = service.generate(legacy.getId());
        assertEquals("1000", result.getQuantumMicrons()); // 0.1cm
        ResultDoc.SlotView bust = result.getSlots().stream()
                .filter(s -> "bust".equals(s.getPartId())).findFirst().orElseThrow();
        assertEquals("86.0", cell(bust, -1).getCm());
        assertEquals("90.0", cell(bust, 0).getCm());
        assertEquals("94.0", cell(bust, 1).getCm());
        // 绝不能被当成毫米放大 10 倍
        assertEquals("940", cell(bust, 1).getMm());

        // 详情也能正常打开
        Map<String, Object> detail = service.detail(legacy.getId());
        assertNotNull(detail.get("spec"));
        assertEquals(2, ((SchemeDoc) detail.get("spec")).getSchemaVersion());
    }

    @Test
    @DisplayName("两方案比较：固定增量不同 → 公式差异并定位尺码")
    void compareTwoSchemes() {
        long a = saveStandard("方案A", "cm");
        SchemeDoc docB = TestSchemes.standard("cm", "cm", "0.1");
        SchemeDoc.PartDoc bust = docB.getParts().stream()
                .filter(p -> "bust".equals(p.getPartId())).findFirst().orElseThrow();
        bust.getRule().setDelta("5"); // 4cm → 5cm
        Map<String, Object> respB = service.saveAndGenerate(null, null, "方案B", null, false, null, docB);
        long b = ((Number) respB.get("id")).longValue();

        Map<String, Object> cmp = service.compare(a, b);
        @SuppressWarnings("unchecked")
        Map<String, Integer> counts = (Map<String, Integer>) cmp.get("counts");
        assertTrue(counts.get("FORMULA") >= 1, "应至少有一格公式差异: " + counts);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> slots = (List<Map<String, Object>>) cmp.get("slots");
        Map<String, Object> bustSlot = slots.stream()
                .filter(s -> "bust".equals(s.get("slotKey"))).findFirst().orElseThrow();
        assertEquals(Boolean.TRUE, bustSlot.get("formulaDifferent"));
    }

    @Test
    @DisplayName("临时计算不落库、互不残留")
    void previewDoesNotPersist() {
        long before = repository.count();
        Map<String, Object> r1 = service.preview(TestSchemes.standard("cm", "cm", "0.1"));
        Map<String, Object> r2 = service.preview(TestSchemes.standard("mm", "cm", "0.1"));
        assertEquals(before, repository.count());
        assertNotNull(r1.get("result"));
        assertNotNull(r2.get("result"));
        // 两次独立请求，结果对象互不影响
        assertTrue(r1.get("contentHash") != null && !r1.get("contentHash").equals(r2.get("contentHash")));
    }

    /* ---------------- helpers ---------------- */

    private ResultDoc.CellView cell(ResultDoc.SlotView slot, int grade) {
        return slot.getCells().stream().filter(c -> c.getGrade() == grade).findFirst().orElseThrow();
    }

    private SchemeDoc.SegmentDoc segment(int from, int to, String delta, String unit) {
        SchemeDoc.SegmentDoc s = new SchemeDoc.SegmentDoc();
        s.setFromGrade(from);
        s.setToGrade(to);
        s.setDelta(delta);
        s.setUnit(unit);
        return s;
    }

    private SchemeDoc withBustDelta(SchemeDoc doc, String delta, String unit) {
        doc.getParts().stream().filter(p -> "bust".equals(p.getPartId())).findFirst()
                .ifPresent(p -> {
                    p.getRule().setDelta(delta);
                    p.getRule().setUnit(unit);
                });
        return doc;
    }

    private SchemeDoc readSpec(GradingScheme entity) {
        try {
            com.fasterxml.jackson.databind.JsonNode root =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(entity.getSpecJson());
            return new com.fasterxml.jackson.databind.ObjectMapper().treeToValue(root, SchemeDoc.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String writeJson(Object o) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(o);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
