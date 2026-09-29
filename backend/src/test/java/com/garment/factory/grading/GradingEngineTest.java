package com.garment.factory.grading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.garment.factory.common.BizException;
import com.garment.factory.grading.engine.AnomalyType;
import com.garment.factory.grading.engine.DecimalQuantity;
import com.garment.factory.grading.engine.Direction;
import com.garment.factory.grading.engine.GradingAnomaly;
import com.garment.factory.grading.engine.GradingGenerator;
import com.garment.factory.grading.engine.GradingPart;
import com.garment.factory.grading.engine.GradingSpec;
import com.garment.factory.grading.engine.LengthUnit;
import com.garment.factory.grading.engine.RuleConfig;
import com.garment.factory.grading.engine.RuleType;
import com.garment.factory.grading.engine.Segment;

/**
 * 放码引擎口径验收：纯引擎、无 Spring / 无数据库。
 */
class GradingEngineTest {

    private static final BigDecimal Q_01CM = BigDecimal.valueOf(1_000); // 0.1cm
    private static final BigDecimal Q_1MM = BigDecimal.valueOf(1_000);  // 1mm（同一量子，写法不同）
    private static final BigDecimal Q_05CM = BigDecimal.valueOf(5_000);

    private static List<GradingSpec.SizeGrade> sizes(int from, int to) {
        List<GradingSpec.SizeGrade> list = new ArrayList<>();
        for (int g = from; g <= to; g++) {
            list.add(new GradingSpec.SizeGrade(g, "G" + g));
        }
        return list;
    }

    private static GradingPart fixedPart(String id, String name, String side, String base,
                                         String delta, LengthUnit unit, Direction dir) {
        RuleConfig rule = new RuleConfig();
        rule.setType(RuleType.FIXED);
        rule.setDeltaMicrons(DecimalQuantity.parseDelta(delta, unit));
        return new GradingPart(id, name, side, DecimalQuantity.parseLength(base, unit), dir, rule);
    }

    private static GradingPart ratioPart(String id, String name, String base, String rate,
                                         LengthUnit unit, Direction dir) {
        RuleConfig rule = new RuleConfig();
        rule.setType(RuleType.RATIO);
        rule.setRate(DecimalQuantity.parseRate(rate));
        return new GradingPart(id, name, null, DecimalQuantity.parseLength(base, unit), dir, rule);
    }

    private static GradingPart segmentPart(String id, String name, String base, LengthUnit unit,
                                           Direction dir, Object... triplets) {
        RuleConfig rule = new RuleConfig();
        rule.setType(RuleType.SEGMENT);
        List<Segment> segs = new ArrayList<>();
        for (int i = 0; i < triplets.length; i += 3) {
            int from = (int) triplets[i];
            int to = (int) triplets[i + 1];
            segs.add(new Segment(from, to, DecimalQuantity.parseDelta((String) triplets[i + 2], unit)));
        }
        rule.setSegments(segs);
        return new GradingPart(id, name, null, DecimalQuantity.parseLength(base, unit), dir, rule);
    }

    @Test
    @DisplayName("规则1 固定增量：升码与降码对称，总变化 = 级差 × 单级增量")
    void fixedRuleUpAndDown() {
        var spec = new GradingSpec(sizes(-2, 2), 0, Q_01CM,
                List.of(fixedPart("bust", "胸围", null, "90", "4", LengthUnit.CM, Direction.UP)),
                List.of());
        var rs = GradingGenerator.generate(spec);

        assertCm(rs.cell("bust", -2).finalMicrons(), "82");
        assertCm(rs.cell("bust", -1).finalMicrons(), "86");
        assertCm(rs.cell("bust", 0).finalMicrons(), "90");
        assertCm(rs.cell("bust", 1).finalMicrons(), "94");
        assertCm(rs.cell("bust", 2).finalMicrons(), "98");
        // 每格都标出相对基准版的总变化
        assertEquals(0, rs.cell("bust", 2).baseDeltaMicrons().compareTo(um("8")));
        assertEquals(0, rs.cell("bust", -2).baseDeltaMicrons().compareTo(um("-8")));
        assertTrue(rs.anomalies().isEmpty());
    }

    @Test
    @DisplayName("规则2 按比例：连乘不逐段舍入；(1+rate)^grade，升降对称")
    void ratioRule() {
        // 基准 100cm，每级 25%：100 → 125 → 156.25（未舍入），量子 0.1cm → 156.3
        var spec = new GradingSpec(sizes(-2, 2), 0, Q_01CM,
                List.of(ratioPart("hip", "臀围", "100", "25%", LengthUnit.CM, Direction.UP)),
                List.of());
        var rs = GradingGenerator.generate(spec);
        assertCm(rs.cell("hip", 2).finalMicrons(), "156.3");
        assertCm(rs.cell("hip", 1).finalMicrons(), "125");
        // 降码：100 / 1.25 = 80；100 / 1.25^2 = 64
        assertCm(rs.cell("hip", -1).finalMicrons(), "80");
        assertCm(rs.cell("hip", -2).finalMicrons(), "64");
        // 未舍入值必须是精确的 156.25（连乘不提前舍入）
        assertEquals(0, rs.cell("hip", 2).rawMicrons().compareTo(um("156.25")));
    }

    @Test
    @DisplayName("规则3 分段：小码段与大码段不同增量，逐段累加")
    void segmentRule() {
        // 基准 90；-2..0 每级 +3（往小码方向减3），0..2 每级 +5
        var spec = new GradingSpec(sizes(-2, 2), 0, Q_01CM,
                List.of(segmentPart("waist", "腰围", "90", LengthUnit.CM, Direction.UP,
                        -2, 0, "3", 0, 2, "5")), List.of());
        var rs = GradingGenerator.generate(spec);
        assertCm(rs.cell("waist", -2).finalMicrons(), "84");
        assertCm(rs.cell("waist", -1).finalMicrons(), "87");
        assertCm(rs.cell("waist", 0).finalMicrons(), "90");
        assertCm(rs.cell("waist", 1).finalMicrons(), "95");
        assertCm(rs.cell("waist", 2).finalMicrons(), "100");
    }

    @Test
    @DisplayName("分段缺口必须整次报错，且错误信息定位到具体尺码边")
    void segmentGapRejected() {
        // 缺边 1→2
        var spec = new GradingSpec(sizes(-2, 2), 0, Q_01CM,
                List.of(segmentPart("waist", "腰围", "90", LengthUnit.CM, Direction.UP,
                        -2, 0, "3", 0, 1, "5")), List.of());
        BizException ex = assertThrows(BizException.class, () -> GradingGenerator.generate(spec));
        assertTrue(ex.getMessage().contains("缺少边 1→2"), ex.getMessage());
        assertTrue(ex.getMessage().contains("G1 → G2"), ex.getMessage());
    }

    @Test
    @DisplayName("左右不对称：L/R 分别计算，不自动合并；两侧结果可以不同")
    void asymmetricSidesCalculatedSeparately() {
        GradingPart left = fixedPart("shoulder", "肩宽", "L", "19.0", "0.4", LengthUnit.CM, Direction.UP);
        GradingPart right = fixedPart("shoulder", "肩宽", "R", "19.2", "0.6", LengthUnit.CM, Direction.UP);
        var spec = new GradingSpec(sizes(-1, 1), 0, Q_01CM, List.of(left, right), List.of());
        var rs = GradingGenerator.generate(spec);

        assertCm(rs.cell("shoulder#L", 1).finalMicrons(), "19.4");
        assertCm(rs.cell("shoulder#R", 1).finalMicrons(), "19.8");
        assertCm(rs.cell("shoulder#L", -1).finalMicrons(), "18.6");
        assertCm(rs.cell("shoulder#R", -1).finalMicrons(), "18.6");
        // 没有任何 "shoulder" 整体槽位被合并出来
        assertEquals(2, rs.parts().size());
    }

    @Test
    @DisplayName("cm 与 mm 输入同值：900mm 与 90cm 内部完全相等，切换单位零误差")
    void cmMmRoundtripExact() {
        var cmPart = fixedPart("bust", "胸围", null, "90", "4", LengthUnit.CM, Direction.UP);
        var mmPart = fixedPart("bust", "胸围", null, "900", "40", LengthUnit.MM, Direction.UP);
        assertEquals(0, cmPart.baseMicrons().compareTo(mmPart.baseMicrons()));
        assertEquals(0, cmPart.rule().getDeltaMicrons().compareTo(mmPart.rule().getDeltaMicrons()));

        var specCm = new GradingSpec(sizes(-1, 1), 0, Q_01CM, List.of(cmPart), List.of());
        var specMm = new GradingSpec(sizes(-1, 1), 0, Q_1MM, List.of(mmPart), List.of());
        var rsCm = GradingGenerator.generate(specCm);
        var rsMm = GradingGenerator.generate(specMm);
        for (int g = -1; g <= 1; g++) {
            assertEquals(0, rsCm.cell("bust", g).finalMicrons()
                    .compareTo(rsMm.cell("bust", g).finalMicrons()));
        }
        // 结果在量子整数上，cm/mm 显示无损互转
        BigDecimal v = rsCm.cell("bust", 1).finalMicrons(); // 940000 μm
        assertEquals("94.0", DecimalQuantity.toDisplay(v, LengthUnit.CM, 1));
        assertEquals("940", DecimalQuantity.toDisplay(v, LengthUnit.MM, 0));
        // 反复往返 100 次：显示值解析回来必须仍是同一个微米整数（无累计漂移）
        BigDecimal cur = v;
        for (int i = 0; i < 100; i++) {
            String cmText = DecimalQuantity.toDisplay(cur, LengthUnit.CM, 1);
            cur = LengthUnit.CM.toMicrons(new BigDecimal(cmText));
            String mmText = DecimalQuantity.toDisplay(cur, LengthUnit.MM, 0);
            cur = LengthUnit.MM.toMicrons(new BigDecimal(mmText));
        }
        assertEquals(0, cur.compareTo(v));
    }

    @Test
    @DisplayName("舍入边界：半进位（四舍五入，半数远离0）")
    void roundingBoundaryHalfUp() {
        // 固定 +4.05cm：90 → 94.05；0.1cm 量子：94.05 = 940.5 个 0.01... 940.5×0.01?
        // 94.05 / 0.1 = 940.5，半数远离 0 → 941 ×0.1 = 94.1
        var part = fixedPart("bust", "胸围", null, "90", "4.05", LengthUnit.CM, Direction.UP);
        var spec = new GradingSpec(sizes(0, 1), 0, Q_01CM, List.of(part), List.of());
        var rs = GradingGenerator.generate(spec);
        assertCm(rs.cell("bust", 1).finalMicrons(), "94.1");

        // 90 - 4.05 = 85.95；85.95/0.1 = 859.5，半数远离 0（正数）→ 860 → 86.0
        var negPart = fixedPart("len", "衣长", null, "90", "-4.05", LengthUnit.CM, Direction.ANY);
        var negSpec = new GradingSpec(sizes(0, 1), 0, Q_01CM, List.of(negPart), List.of());
        var negRs = GradingGenerator.generate(negSpec);
        assertCm(negRs.cell("len", 1).finalMicrons(), "86.0");

        // 真正的负数对称：base 10cm，+? 用降码方向验证 -0.05 边界
        // base 90、delta -4.05 在级差 -1（降码反向）= 90 + 4.05 = 94.05；
        // 这里直接造一个负值结果：base 1cm 减 1.05cm = -0.05cm，/0.1=-0.5 → 远离0 → -1×0.1=-0.1
        var negVal = fixedPart("nv", "负值", null, "1", "-1.05", LengthUnit.CM, Direction.ANY);
        var nvSpec = new GradingSpec(sizes(0, 1), 0, Q_01CM, List.of(negVal), List.of());
        var nvRs = GradingGenerator.generate(nvSpec);
        assertCm(nvRs.cell("nv", 1).finalMicrons(), "-0.1");

        // 0.5cm 量子下 94.05 → 94.0（距 94.0 更近）
        var spec05 = new GradingSpec(sizes(0, 1), 0, Q_05CM, List.of(
                fixedPart("bust", "胸围", null, "90", "4.05", LengthUnit.CM, Direction.UP)), List.of());
        assertCm(GradingGenerator.generate(spec05).cell("bust", 1).finalMicrons(), "94.0");
    }

    @Test
    @DisplayName("异常定位：负值 / 零长度 / 倒挂 / 增量方向错误 / 肩胸比异常，均定位到尺码+部位+原因")
    void anomaliesLocated() {
        GradingPart bust = fixedPart("bust", "胸围", null, "90", "4", LengthUnit.CM, Direction.UP);
        // 方向错误：声明 UP，增量却是 -2
        GradingPart len = fixedPart("bodyLen", "衣长", null, "60", "-2", LengthUnit.CM, Direction.UP);
        // 负值：基准 2cm，升一级 = 2 + (-3) = -1cm（delta -30mm），方向 ANY 隔离 NEGATIVE
        GradingPart tiny = fixedPart("tiny", "微调", null, "2", "-30", LengthUnit.MM, Direction.ANY);
        var check = new com.garment.factory.grading.engine.RatioCheck(
                "SHOULDER_CHEST", "shoulder", "bust",
                new BigDecimal("0.35"), new BigDecimal("0.50"), "肩宽/胸围");
        GradingPart shoulder = fixedPart("shoulder", "肩宽", null, "60", "2", LengthUnit.CM, Direction.UP);

        var spec = new GradingSpec(sizes(-1, 1), 0, Q_01CM,
                List.of(bust, len, tiny, shoulder), List.of(check));
        var rs = GradingGenerator.generate(spec);
        List<GradingAnomaly> anomalies = rs.anomalies();

        // 负值定位：tiny 升 1 级 = 2cm - 3cm = -1cm
        assertTrue(anomalies.stream().anyMatch(a -> a.type() == AnomalyType.NEGATIVE
                && a.grade() == 1 && "tiny".equals(a.slotKey())
                && a.reason().contains("最终尺寸为负")));

        // 增量方向错误：定位到边（首条边 -1→0）
        GradingAnomaly dir = anomalies.stream()
                .filter(a -> a.type() == AnomalyType.INCREMENT_DIRECTION).findFirst().orElseThrow();
        assertEquals("bodyLen", dir.slotKey());
        assertEquals(-1, dir.edgeFrom());
        assertEquals(0, dir.edgeTo());
        assertTrue(dir.reason().contains("升码增大"));

        // 倒挂：衣长相邻尺码 62→60→58，声明 UP 却一路减小
        assertTrue(anomalies.stream().anyMatch(a -> a.type() == AnomalyType.INVERSION
                && "bodyLen".equals(a.slotKey())));

        // 肩胸比异常：基准 60/90 = 0.666667 > 0.50，定位尺码
        assertTrue(anomalies.stream().anyMatch(a -> a.type() == AnomalyType.RATIO_ABNORMAL
                && "shoulder".equals(a.slotKey()) && a.reason().contains("0.666667")));
    }

    @Test
    @DisplayName("比例因子非正（如 -100%）属于公式无法计算，整次生成直接失败")
    void invalidRatioFails() {
        GradingPart p = ratioPart("x", "X", "100", "-100%", LengthUnit.CM, Direction.ANY);
        var spec = new GradingSpec(sizes(0, 1), 0, Q_01CM, List.of(p), List.of());
        assertThrows(BizException.class, () -> GradingGenerator.generate(spec));
    }

    @Test
    @DisplayName("确定性：同方案生成两次，全部数值与异常逐条相等")
    void deterministic() {
        var spec = new GradingSpec(sizes(-2, 2), 0, Q_01CM,
                List.of(segmentPart("waist", "腰围", "90", LengthUnit.CM, Direction.UP,
                        -2, 0, "3", 0, 2, "5"),
                        ratioPart("hip", "臀围", "100", "4%", LengthUnit.CM, Direction.UP)),
                List.of());
        var a = GradingGenerator.generate(spec);
        var b = GradingGenerator.generate(spec);
        for (var size : spec.sizes()) {
            for (var part : spec.parts()) {
                var ca = a.cell(part.slotKey(), size.grade());
                var cb = b.cell(part.slotKey(), size.grade());
                assertEquals(0, ca.finalMicrons().compareTo(cb.finalMicrons()));
                assertEquals(0, ca.rawMicrons().compareTo(cb.rawMicrons()));
                assertEquals(0, ca.baseDeltaMicrons().compareTo(cb.baseDeltaMicrons()));
            }
        }
        assertEquals(a.anomalies().size(), b.anomalies().size());
    }

    @Test
    @DisplayName("性能：200 尺码 × 100 槽位（2 万格，分段规则）在 1 秒内完成")
    void performance() {
        List<GradingPart> parts = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            String id = "p" + i;
            parts.add(segmentPart(id, "部位" + i, "90", LengthUnit.CM, Direction.UP,
                    -100, 0, "2.5", 0, 100, "4.5"));
        }
        var spec = new GradingSpec(sizes(-100, 99), 0, Q_01CM, parts, List.of());
        long start = System.nanoTime();
        var rs = GradingGenerator.generate(spec);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        assertEquals(200 * 100, rs.cells().size());
        assertTrue(elapsedMs < 1_000, "2万格计算耗时 " + elapsedMs + "ms，超过 1s 预算");
    }

    @Test
    @DisplayName("零长度：基准100mm 升一级 -100mm 恰好为 0 时报 ZERO_LENGTH")
    void zeroLength() {
        var p = fixedPart("z", "Z", null, "100", "-100", LengthUnit.MM, Direction.ANY);
        var spec = new GradingSpec(sizes(0, 1), 0, Q_1MM, List.of(p), List.of());
        var rs = GradingGenerator.generate(spec);
        assertTrue(rs.anomalies().stream().anyMatch(a -> a.type() == AnomalyType.ZERO_LENGTH && a.grade() == 1));
    }

    @Test
    @DisplayName("任何中间过程都不在换单位时舍入：raw 保留精确值，只在最终量子上舍入")
    void noIntermediateRounding() {
        // 3.33cm 增量 × 3 级 = 9.99cm；0.1cm 量子 → 10.0
        var p = fixedPart("b", "B", null, "100", "3.33", LengthUnit.CM, Direction.UP);
        var cmSpec = new GradingSpec(sizes(0, 3), 0, new BigDecimal("1000"), List.of(p), List.of());
        var rs = GradingGenerator.generate(cmSpec);
        assertCm(rs.cell("b", 3).finalMicrons(), "110"); // 109.99 → 110.0
        // 未舍入值保留精确 109.99，证明中间过程没有提前舍入
        assertEquals(0, rs.cell("b", 3).rawMicrons().compareTo(um("109.99")));
    }

    /* ---- 辅助 ---- */
    /** 按厘米数值比较（忽略 86 与 86.0 的尾随零差异）。 */
    private static void assertCm(BigDecimal microns, String expectedCm) {
        BigDecimal actual = microns.divide(LengthUnit.CM.micronFactor());
        assertEquals(0, new BigDecimal(expectedCm).compareTo(actual),
                "期望 " + expectedCm + "cm，实际 " + actual.toPlainString() + "cm");
    }

    private static BigDecimal um(String cmValue) {
        return LengthUnit.CM.toMicrons(new BigDecimal(cmValue));
    }
}
