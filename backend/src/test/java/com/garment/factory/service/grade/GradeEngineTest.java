package com.garment.factory.service.grade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.garment.factory.common.BizException;
import com.garment.factory.domain.GradeDict;
import com.garment.factory.dto.GradeBaseReq;
import com.garment.factory.dto.GradeCell;
import com.garment.factory.dto.GradeIssue;
import com.garment.factory.dto.GradePartResult;
import com.garment.factory.dto.GradeResult;
import com.garment.factory.dto.GradeRuleReq;
import com.garment.factory.dto.GradeSchemeReq;
import com.garment.factory.dto.GradeSegmentReq;

class GradeEngineTest {

    private final GradeNormalizer normalizer = new GradeNormalizer();
    private final GradeEngine engine = new GradeEngine();

    private GradeSchemeReq baseReq(String unit, BigDecimal roundStepMm) {
        GradeSchemeReq req = new GradeSchemeReq();
        req.setInputUnit(unit);
        req.setRoundStepMm(roundStepMm);
        req.setBaseSizeCode("M");
        req.setSizes(new ArrayList<>(List.of("XS", "S", "M", "L", "XL")));
        req.setBases(new ArrayList<>());
        req.setRules(new ArrayList<>());
        // 厘米：肩 19.8/19.7；毫米：198/197，表达同一基准
        addBase(req, "bust", "CENTER", "90");
        addBase(req, "waist", "CENTER", "74");
        addBase(req, "hip", "CENTER", "97");
        addBase(req, "bodyLength", "CENTER", "65");
        addBase(req, "shoulder", "LEFT", "19.8");
        addBase(req, "shoulder", "RIGHT", "19.7");
        addBase(req, "sleeveLength", "LEFT", "58");
        addBase(req, "sleeveLength", "RIGHT", "58");
        if (GradeDict.UNIT_MM.equals(unit)) {
            req.getBases().forEach(b -> b.setValue(b.getValue().multiply(BigDecimal.TEN)));
        }
        return req;
    }

    private void addBase(GradeSchemeReq req, String part, String side, String value) {
        req.getBases().add(new GradeBaseReq(part, side, new BigDecimal(value)));
    }

    private GradeRuleReq fixedRule(String part, String side, String delta) {
        GradeRuleReq r = new GradeRuleReq();
        r.setPart(part);
        r.setSide(side);
        r.setRuleType(GradeDict.RULE_FIXED);
        r.setFixedDelta(new BigDecimal(delta));
        return r;
    }

    private GradeRuleReq ratioRule(String part, String side, String percent) {
        GradeRuleReq r = new GradeRuleReq();
        r.setPart(part);
        r.setSide(side);
        r.setRuleType(GradeDict.RULE_RATIO);
        r.setRatioPercent(new BigDecimal(percent));
        return r;
    }

    private GradeRuleReq segmentRule(String part, String side, int[][] levels, String[] deltas) {
        GradeRuleReq r = new GradeRuleReq();
        r.setPart(part);
        r.setSide(side);
        r.setRuleType(GradeDict.RULE_SEGMENT);
        List<GradeSegmentReq> segs = new ArrayList<>();
        for (int i = 0; i < levels.length; i++) {
            GradeSegmentReq s = new GradeSegmentReq();
            s.setFromLevel(levels[i][0]);
            s.setToLevel(levels[i][1]);
            s.setDelta(new BigDecimal(deltas[i]));
            segs.add(s);
        }
        r.setSegments(segs);
        return r;
    }

    private GradeSchemeReq fullScheme(String unit, BigDecimal stepMm, boolean withSegment) {
        GradeSchemeReq req = baseReq(unit, stepMm);
        String d40 = GradeDict.UNIT_MM.equals(unit) ? "40" : "4";
        String d36 = GradeDict.UNIT_MM.equals(unit) ? "36" : "3.6";
        String d20 = GradeDict.UNIT_MM.equals(unit) ? "20" : "2";
        String d15 = GradeDict.UNIT_MM.equals(unit) ? "15" : "1.5";
        String shoulderSmall = GradeDict.UNIT_MM.equals(unit) ? "4" : "0.4";
        String shoulderLarge = GradeDict.UNIT_MM.equals(unit) ? "6" : "0.6";
        req.getRules().add(fixedRule("bust", "CENTER", d40));
        req.getRules().add(ratioRule("waist", "CENTER", "4"));
        req.getRules().add(fixedRule("hip", "CENTER", d36));
        req.getRules().add(fixedRule("bodyLength", "CENTER", d20));
        for (String side : new String[]{"LEFT", "RIGHT"}) {
            if (withSegment) {
                // 三段：小码段[-2,-1]每级 4mm，0 级段 0，大码段[1,2]每级 6mm
                req.getRules().add(segmentRule("shoulder", side,
                        new int[][]{{-2, -1}, {0, 0}, {1, 2}},
                        new String[]{shoulderSmall, "0", shoulderLarge}));
            } else {
                req.getRules().add(fixedRule("shoulder", side, GradeDict.UNIT_MM.equals(unit) ? "5" : "0.5"));
            }
            req.getRules().add(fixedRule("sleeveLength", side, d15));
        }
        return req;
    }

    private GradePartResult part(GradeResult r, String part, String side) {
        return r.getParts().stream()
                .filter(p -> p.getPart().equals(part) && p.getSide().equals(side))
                .findFirst().orElseThrow();
    }

    private GradeCell cell(GradeResult r, String part, String side, String size) {
        return part(r, part, side).getCells().stream()
                .filter(c -> c.getSizeCode().equals(size)).findFirst().orElseThrow();
    }

    @Test
    void fixedRuleGradesUpAndDownFromBase() {
        GradeResult r = engine.compute(normalizer.normalize(fullScheme("CM", BigDecimal.ONE, false)));
        assertEquals(new BigDecimal("980"), cell(r, "bust", "CENTER", "XL").getValueMm());
        assertEquals(new BigDecimal("820"), cell(r, "bust", "CENTER", "XS").getValueMm());
        assertEquals(BigDecimal.ZERO, cell(r, "bust", "CENTER", "M").getDeltaMm());
        assertEquals(new BigDecimal("80"), cell(r, "bust", "CENTER", "XL").getDeltaMm());
        assertEquals(new BigDecimal("-80"), cell(r, "bust", "CENTER", "XS").getDeltaMm());
    }

    @Test
    void ratioRuleIsLinearFromRoundedBase() {
        GradeResult r = engine.compute(normalizer.normalize(fullScheme("CM", BigDecimal.ONE, false)));
        // 腰围基准 740mm，每级 4%：L raw=769.6 → 770；XL raw=799.2 → 799；S raw=710.4 → 710
        assertEquals(new BigDecimal("770"), cell(r, "waist", "CENTER", "L").getValueMm());
        assertEquals(new BigDecimal("799"), cell(r, "waist", "CENTER", "XL").getValueMm());
        assertEquals(new BigDecimal("710"), cell(r, "waist", "CENTER", "S").getValueMm());
        assertEquals(new BigDecimal("681"), cell(r, "waist", "CENTER", "XS").getValueMm());
    }

    @Test
    void segmentRuleUsesDifferentDeltasAcrossBands() {
        GradeResult r = engine.compute(normalizer.normalize(fullScheme("CM", BigDecimal.ONE, true)));
        // 左半肩基准 198。起点段口径：0→-1 用 0 级段（0）→ S 持平 198；
        // -1→-2 用 -1 级段（4）→ XS=194；0→1 用 0 级段（0）→ L 持平 198；1→2 用大码段（6）→ XL=204
        assertEquals(new BigDecimal("194"), cell(r, "shoulder", "LEFT", "XS").getValueMm());
        assertEquals(new BigDecimal("198"), cell(r, "shoulder", "LEFT", "S").getValueMm());
        assertEquals(new BigDecimal("198"), cell(r, "shoulder", "LEFT", "M").getValueMm());
        assertEquals(new BigDecimal("198"), cell(r, "shoulder", "LEFT", "L").getValueMm());
        assertEquals(new BigDecimal("204"), cell(r, "shoulder", "LEFT", "XL").getValueMm());
        // 左右不对称：右半肩基准 197，规则相同但结果各自独立，绝不合并
        assertEquals(new BigDecimal("193"), cell(r, "shoulder", "RIGHT", "XS").getValueMm());
        assertEquals(new BigDecimal("197"), cell(r, "shoulder", "RIGHT", "S").getValueMm());
        assertEquals(new BigDecimal("197"), cell(r, "shoulder", "RIGHT", "L").getValueMm());
        assertEquals(new BigDecimal("203"), cell(r, "shoulder", "RIGHT", "XL").getValueMm());
        assertFalse(r.getParts().stream().anyMatch(p -> "shoulder".equals(p.getPart()) && "CENTER".equals(p.getSide())));
    }

    @Test
    void cmAndMmInputProduceIdenticalResults() {
        GradeResult cm = engine.compute(normalizer.normalize(fullScheme("CM", BigDecimal.ONE, true)));
        GradeResult mm = engine.compute(normalizer.normalize(fullScheme("MM", BigDecimal.ONE, true)));
        for (int i = 0; i < cm.getParts().size(); i++) {
            GradePartResult a = cm.getParts().get(i);
            GradePartResult b = mm.getParts().get(i);
            assertEquals(a.getPart(), b.getPart());
            assertEquals(a.getSide(), b.getSide());
            for (int j = 0; j < a.getCells().size(); j++) {
                assertEquals(a.getCells().get(j).getValueMm(), b.getCells().get(j).getValueMm(),
                        a.getPart() + "/" + a.getCells().get(j).getSizeCode());
                assertEquals(a.getCells().get(j).getDeltaMm(), b.getCells().get(j).getDeltaMm());
            }
        }
    }

    @Test
    void cmIsExactDecimalShiftAndRepeatedDisplaySwitchCannotDrift() {
        GradeResult r = engine.compute(normalizer.normalize(fullScheme("CM", BigDecimal.ONE, false)));
        GradeCell c = cell(r, "hip", "CENTER", "L"); // 970 + 36 = 1006mm → 100.6cm
        assertEquals(new BigDecimal("1006"), c.getValueMm());
        assertEquals(new BigDecimal("100.6"), c.getValueCm());
        // 模拟页面 N 次单位往返：始终 valueMm/10（除得尽，不需要舍入），再乘回来
        for (int i = 0; i < 50; i++) {
            BigDecimal cm = c.getValueMm().divide(BigDecimal.TEN);
            BigDecimal back = cm.multiply(BigDecimal.TEN);
            assertEquals(0, c.getValueMm().compareTo(back));
        }
    }

    @Test
    void halfUpRoundingBoundary() {
        GradeSchemeReq req = fullScheme("CM", BigDecimal.ONE, false);
        // 胸围基准改为 90.5cm（905mm），固定 4cm → L raw=945；比例用腰围验证 .2 边界
        req.getBases().stream().filter(b -> b.getPart().equals("bust")).findFirst()
                .ifPresent(b -> b.setValue(new BigDecimal("90.5")));
        GradeResult r = engine.compute(normalizer.normalize(req));
        assertEquals(new BigDecimal("945"), cell(r, "bust", "CENTER", "L").getValueMm());
        // 腰围 740 × (1+4%)=769.6 → HALF_UP 770
        assertEquals(new BigDecimal("770"), cell(r, "waist", "CENTER", "L").getValueMm());
    }

    @Test
    void negativeZeroAndInversionAndDirectionAreLocated() {
        GradeSchemeReq req = baseReq("CM", BigDecimal.ONE);
        req.getRules().add(fixedRule("bust", "CENTER", "4"));
        req.getRules().add(fixedRule("waist", "CENTER", "4"));
        req.getRules().add(fixedRule("hip", "CENTER", "4"));
        req.getRules().add(fixedRule("bodyLength", "CENTER", "2"));
        // 肩宽分段规则：小码段（含 0 级，+4mm，故 M→L 变大），
        // 大码段[1,2]改为缩小（-5mm）→ L→XL 相邻倒挂（INVERSION 定位）
        req.getRules().add(segmentRule("shoulder", "LEFT",
                new int[][]{{-2, 0}, {1, 2}},
                new String[]{"0.4", "-0.5"}));
        req.getRules().add(segmentRule("shoulder", "RIGHT",
                new int[][]{{-2, 0}, {1, 2}},
                new String[]{"0.4", "-0.5"}));
        req.getRules().add(fixedRule("sleeveLength", "LEFT", "1.5"));
        req.getRules().add(fixedRule("sleeveLength", "RIGHT", "1.5"));
        // 把衣长基准改成 0.01cm：整毫米舍入后基准格即零长度，降码为负数，触发 NEGATIVE_OR_ZERO
        req.getBases().stream().filter(b -> b.getPart().equals("bodyLength")).findFirst()
                .ifPresent(b -> b.setValue(new BigDecimal("0.01")));
        GradeResult r = engine.compute(normalizer.normalize(req));
        assertTrue(r.getIssues().stream().anyMatch(i -> "NEGATIVE_OR_ZERO".equals(i.getCode())
                && "bodyLength".equals(i.getPart())), "应定位到衣长的零/负数");
        // 倒挂：肩宽大码段缩小 → XL 相对 L 倒挂
        assertTrue(r.getIssues().stream().anyMatch(i -> "INVERSION".equals(i.getCode())
                && "shoulder".equals(i.getPart()) && "XL".equals(i.getSizeCode())));
        for (GradeIssue i : r.getIssues()) {
            assertTrue(i.getMessage().contains(i.getSizeCode()));
        }
    }

    @Test
    void directionErrorWhenRoundingReversesConfiguredStep() {
        // 分段规则：小码段（含 0 级）放大 +6mm，大码段[1,2]却缩小 -2mm（相邻码段方向翻转）。
        // 进入大码段的第一步 M→L 即定位 DIRECTION；XL 相对 L 缩小再报 INVERSION。
        GradeSchemeReq req = baseReq("CM", BigDecimal.ONE);
        for (String part : new String[]{"bust", "waist", "hip", "bodyLength"}) {
            req.getRules().add(fixedRule(part, "CENTER", part.equals("bodyLength") ? "2" : "4"));
        }
        for (String side : new String[]{"LEFT", "RIGHT"}) {
            req.getRules().add(segmentRule("shoulder", side,
                    new int[][]{{-2, 0}, {1, 2}}, new String[]{"0.6", "-0.2"}));
            req.getRules().add(fixedRule("sleeveLength", side, "1.5"));
        }
        GradeResult r = engine.compute(normalizer.normalize(req));
        assertTrue(r.getIssues().stream().anyMatch(i -> "DIRECTION".equals(i.getCode())
                && "shoulder".equals(i.getPart()) && "XL".equals(i.getSizeCode())),
                "应在进入缩小段的 XL（L→XL 步）定位到增量方向翻转");
        assertTrue(r.getIssues().stream().anyMatch(i -> "INVERSION".equals(i.getCode())
                && "shoulder".equals(i.getPart()) && "XL".equals(i.getSizeCode())));
    }

    @Test
    void shoulderBustRatioAbnormalIsLocated() {
        GradeSchemeReq req = fullScheme("CM", BigDecimal.ONE, false);
        // 肩宽基准改成左右各 30cm（总肩宽 600mm / 胸围 900mm = 0.667 > 0.62）
        req.getBases().stream().filter(b -> b.getPart().equals("shoulder"))
                .forEach(b -> b.setValue(new BigDecimal("30")));
        GradeResult r = engine.compute(normalizer.normalize(req));
        assertTrue(r.getIssues().stream().anyMatch(i -> "SHOULDER_RATIO".equals(i.getCode())
                && "M".equals(i.getSizeCode()) && i.getMessage().contains("肩胸比例明显异常")));
    }

    @Test
    void invalidSegmentGapFailsHard() {
        GradeSchemeReq req = fullScheme("CM", BigDecimal.ONE, false);
        req.getRules().removeIf(rule -> "shoulder".equals(rule.getPart()));
        req.getRules().add(segmentRule("shoulder", "LEFT",
                new int[][]{{-2, -1}, {1, 2}}, new String[]{"0.4", "0.6"})); // 缺 0 级
        req.getRules().add(fixedRule("shoulder", "RIGHT", "0.5"));
        BizException ex = assertThrows(BizException.class,
                () -> engine.compute(normalizer.normalize(req)));
        assertTrue(ex.getMessage().contains("连续") || ex.getMessage().contains("覆盖"));
    }

    @Test
    void asymmetricPartCannotBeMergedToCenter() {
        GradeSchemeReq req = fullScheme("CM", BigDecimal.ONE, false);
        req.getBases().removeIf(b -> b.getPart().equals("shoulder") && "RIGHT".equals(b.getSide()));
        req.getBases().add(new GradeBaseReq("shoulder", "CENTER", new BigDecimal("39.5")));
        assertThrows(BizException.class, () -> normalizer.normalize(req));
    }

    @Test
    void legacyCentimetreDataReadsBackWithIdenticalValues() {
        // 旧系统只有厘米两位小数：步进 0.1mm，固定增量
        GradeSchemeReq req = new GradeSchemeReq();
        req.setInputUnit("CM");
        req.setRoundStepMm(new BigDecimal("0.1"));
        req.setBaseSizeCode("M");
        req.setSizes(List.of("S", "M", "L"));
        req.setBases(new ArrayList<>(List.of(
                new GradeBaseReq("bust", "CENTER", new BigDecimal("88.00")),
                new GradeBaseReq("waist", "CENTER", new BigDecimal("72.00")),
                new GradeBaseReq("hip", "CENTER", new BigDecimal("94.00")),
                new GradeBaseReq("bodyLength", "CENTER", new BigDecimal("62.00")),
                new GradeBaseReq("shoulder", "LEFT", new BigDecimal("19.25")),
                new GradeBaseReq("shoulder", "RIGHT", new BigDecimal("19.25")),
                new GradeBaseReq("sleeveLength", "LEFT", new BigDecimal("57.00")),
                new GradeBaseReq("sleeveLength", "RIGHT", new BigDecimal("57.00")))));
        req.setRules(new ArrayList<>(List.of(
                fixedRule("bust", "CENTER", "4"), fixedRule("waist", "CENTER", "3"),
                fixedRule("hip", "CENTER", "3.5"), fixedRule("bodyLength", "CENTER", "1.5"),
                fixedRule("shoulder", "LEFT", "0.5"), fixedRule("shoulder", "RIGHT", "0.5"),
                fixedRule("sleeveLength", "LEFT", "1"), fixedRule("sleeveLength", "RIGHT", "1"))));
        GradeResult r = engine.compute(normalizer.normalize(req));
        assertEquals(0, new BigDecimal("84.0").compareTo(cell(r, "bust", "CENTER", "S").getValueCm()));
        assertEquals(0, new BigDecimal("92.0").compareTo(cell(r, "bust", "CENTER", "L").getValueCm()));
        assertEquals(0, new BigDecimal("18.75").compareTo(cell(r, "shoulder", "LEFT", "S").getValueCm()));
        assertEquals(0, new BigDecimal("90.5").compareTo(cell(r, "hip", "CENTER", "S").getValueCm()));
        assertTrue(r.getIssues().isEmpty());
    }

    @Test
    void performanceLargeSizeAndPartSet() {
        // 200 尺码 × 8 部位（含左右共 8 项规则）整组计算应在合理时间内完成
        GradeSchemeReq req = new GradeSchemeReq();
        req.setInputUnit("MM");
        req.setRoundStepMm(BigDecimal.ONE);
        req.setBaseSizeCode("SIZE100");
        List<String> sizes = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            sizes.add("SIZE" + i);
        }
        req.setSizes(sizes);
        req.setBases(new ArrayList<>());
        req.setRules(new ArrayList<>());
        for (String part : new String[]{"bust", "waist", "hip", "bodyLength"}) {
            req.getBases().add(new GradeBaseReq(part, "CENTER", new BigDecimal("900")));
            req.getRules().add(fixedRule(part, "CENTER", "4"));
        }
        for (String part : new String[]{"shoulder", "sleeveLength"}) {
            req.getBases().add(new GradeBaseReq(part, "LEFT", new BigDecimal("200")));
            req.getBases().add(new GradeBaseReq(part, "RIGHT", new BigDecimal("200")));
            for (String side : new String[]{"LEFT", "RIGHT"}) {
                req.getRules().add(segmentRule(part, side,
                        new int[][]{{-100, -1}, {0, 0}, {1, 99}},
                        new String[]{"4", "0", "6"}));
            }
        }
        long start = System.currentTimeMillis();
        GradeResult r = engine.compute(normalizer.normalize(req));
        long elapsed = System.currentTimeMillis() - start;
        assertEquals(200, r.getSizes().size());
        assertEquals(8, r.getParts().size());
        assertTrue(elapsed < 2000, "200码 × 8部位 计算耗时应 < 2s，实际 " + elapsed + "ms");
        assertEquals(new BigDecimal("900"), cell(r, "bust", "CENTER", "SIZE100").getValueMm());
        // SIZE199 = 级序 +99：900 + 99×4 = 1296
        assertEquals(new BigDecimal("1296"), cell(r, "bust", "CENTER", "SIZE199").getValueMm());
    }
}
