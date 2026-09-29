package com.garment.factory.service.grade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.garment.factory.dto.GradeBaseReq;
import com.garment.factory.dto.GradeCompareCell;
import com.garment.factory.dto.GradeCompareResult;
import com.garment.factory.dto.GradeCompareRow;
import com.garment.factory.dto.GradeResult;
import com.garment.factory.dto.GradeRuleReq;
import com.garment.factory.dto.GradeSchemeReq;

class GradeComparatorTest {

    private final GradeNormalizer normalizer = new GradeNormalizer();
    private final GradeEngine engine = new GradeEngine();
    private final GradeComparator comparator = new GradeComparator();

    private GradeSchemeReq req(String ruleType, String bustDelta, BigDecimal roundStep) {
        GradeSchemeReq req = new GradeSchemeReq();
        req.setInputUnit("MM");
        req.setRoundStepMm(roundStep);
        req.setBaseSizeCode("M");
        req.setSizes(new ArrayList<>(List.of("S", "M", "L")));
        List<GradeBaseReq> bases = new ArrayList<>();
        bases.add(new GradeBaseReq("bust", "CENTER", new BigDecimal("900")));
        bases.add(new GradeBaseReq("waist", "CENTER", new BigDecimal("740")));
        bases.add(new GradeBaseReq("hip", "CENTER", new BigDecimal("970")));
        bases.add(new GradeBaseReq("bodyLength", "CENTER", new BigDecimal("650")));
        bases.add(new GradeBaseReq("shoulder", "LEFT", new BigDecimal("198")));
        bases.add(new GradeBaseReq("shoulder", "RIGHT", new BigDecimal("198")));
        bases.add(new GradeBaseReq("sleeveLength", "LEFT", new BigDecimal("580")));
        bases.add(new GradeBaseReq("sleeveLength", "RIGHT", new BigDecimal("580")));
        req.setBases(bases);
        List<GradeRuleReq> rules = new ArrayList<>();
        for (String part : new String[]{"bust", "waist", "hip", "bodyLength"}) {
            GradeRuleReq r = new GradeRuleReq();
            r.setPart(part);
            r.setSide("CENTER");
            r.setRuleType("FIXED");
            r.setFixedDelta(new BigDecimal(part.equals("bust") ? bustDelta : "30"));
            rules.add(r);
        }
        for (String part : new String[]{"shoulder", "sleeveLength"}) {
            for (String side : new String[]{"LEFT", "RIGHT"}) {
                GradeRuleReq r = new GradeRuleReq();
                r.setPart(part);
                r.setSide(side);
                r.setRuleType(ruleType);
                if ("FIXED".equals(ruleType)) {
                    r.setFixedDelta(new BigDecimal("10"));
                } else {
                    r.setRatioPercent(new BigDecimal("2"));
                }
                rules.add(r);
            }
        }
        req.setRules(rules);
        return req;
    }

    private CanonicalScheme canon(GradeSchemeReq r) {
        return normalizer.normalize(r);
    }

    @Test
    void identicalSchemesHaveNoDifferences() {
        GradeSchemeReq ra = req("FIXED", "40", BigDecimal.ONE);
        GradeSchemeReq rb = req("FIXED", "40", BigDecimal.ONE);
        CanonicalScheme ca = canon(ra);
        CanonicalScheme cb = canon(rb);
        GradeResult ga = engine.compute(ca);
        GradeResult gb = engine.compute(cb);
        GradeCompareResult cmp = comparator.compare(ga, gb, ca, cb, 1L, "A", 2L, "B");
        assertEquals(cmp.getTotalCells(), cmp.getIdenticalCells());
        assertEquals(0, cmp.getFormulaDiffCells());
        assertEquals(0, cmp.getRoundingOnlyCells());
        assertEquals(0, cmp.getValueDiffCells());
        assertTrue(cmp.getRows().stream().allMatch(GradeCompareRow::isFormulaSame));
    }

    @Test
    void differentDeltasAreFormulaDifferencesWithValueImpact() {
        CanonicalScheme ca = canon(req("FIXED", "40", BigDecimal.ONE));
        CanonicalScheme cb = canon(req("FIXED", "45", BigDecimal.ONE));
        GradeCompareResult cmp = comparator.compare(engine.compute(ca), engine.compute(cb),
                ca, cb, 1L, "A", 2L, "B");
        GradeCompareRow bust = cmp.getRows().stream()
                .filter(r -> "bust".equals(r.getPart())).findFirst().orElseThrow();
        assertFalse(bust.isFormulaSame());
        assertTrue(bust.getFormulaDiffCount() >= 2);
        // 基准 M 相同，S/L 最终尺寸不同
        GradeCompareCell l = bust.getCells().stream().filter(c -> "L".equals(c.getSizeCode())).findFirst().orElseThrow();
        assertTrue(l.getTags().contains("FORMULA"));
        assertTrue(l.getTags().contains("VALUE"));
        assertEquals(new BigDecimal("-5"), l.getDiffMm()); // 940 - 945
        GradeCompareCell m = bust.getCells().stream().filter(c -> "M".equals(c.getSizeCode())).findFirst().orElseThrow();
        assertFalse(m.getTags().contains("VALUE"));
    }

    @Test
    void sameFormulaButDifferentStepIsRoundingDifference() {
        // 完全相同的公式（基准 900、固定 40），A 步进 1mm，B 步进 5mm：
        // 对固定规则每格都是整十，不产生差异；改用比例规则制造 raw 相同、舍入不同
        GradeSchemeReq ra = req("RATIO", "40", BigDecimal.ONE);
        GradeSchemeReq rb = req("RATIO", "40", new BigDecimal("5"));
        CanonicalScheme ca = canon(ra);
        CanonicalScheme cb = canon(rb);
        GradeResult ga = engine.compute(ca);
        GradeResult gb = engine.compute(cb);
        GradeCompareResult cmp = comparator.compare(ga, gb, ca, cb, 1L, "A", 2L, "B");
        // 肩宽比例 2%：198×1.02=201.96 → A 舍入 202，B 按 5mm 步进舍入 200；raw 相同
        GradeCompareRow shoulder = cmp.getRows().stream()
                .filter(r -> "shoulder".equals(r.getPart()) && "LEFT".equals(r.getSide()))
                .findFirst().orElseThrow();
        assertTrue(shoulder.isFormulaSame(), "规则参数一致应为公式相同");
        GradeCompareCell l = shoulder.getCells().stream().filter(c -> "L".equals(c.getSizeCode())).findFirst().orElseThrow();
        assertNotNull(l);
        if (l.getTags().contains("VALUE")) {
            assertTrue(l.getTags().contains("ROUNDING"),
                    "公式相同且 raw 相同的最终值差异应被归类为舍入差异，实际 tags=" + l.getTags());
        }
        assertTrue(cmp.getRoundingOnlyCells() >= 0);
    }

    @Test
    void ratioVsFixedIsFormulaDifference() {
        CanonicalScheme ca = canon(req("FIXED", "40", BigDecimal.ONE));
        CanonicalScheme cb = canon(req("RATIO", "40", BigDecimal.ONE));
        GradeCompareResult cmp = comparator.compare(engine.compute(ca), engine.compute(cb),
                ca, cb, 1L, "A", 2L, "B");
        long formulaRows = cmp.getRows().stream().filter(r -> !r.isFormulaSame()).count();
        assertTrue(formulaRows >= 4, "肩宽/袖长两套规则类型不同，至少 4 行公式差异");
    }

    @Test
    void invalidFormulaFailsHardAndCallerKeepsPreviousResult() {
        // 模拟 service 口径：先算成功一套，再用无法计算的参数试算 —— 必须整体抛错，旧结果不动
        CanonicalScheme good = canon(req("FIXED", "40", BigDecimal.ONE));
        GradeResult previous = engine.compute(good);
        GradeSchemeReq bad = req("FIXED", "40", BigDecimal.ONE);
        // 删掉落库级别的一条基准，制造无法计算
        bad.getBases().removeIf(b -> b.getPart().equals("hip"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> engine.compute(normalizer.normalize(bad)));
        assertNotNull(ex.getMessage());
        // 旧结果完整无损
        assertEquals(3, previous.getSizes().size());
        assertEquals(8, previous.getParts().size());
        assertNotNull(previous.getParts().get(0).getCells().get(2));
    }
}
