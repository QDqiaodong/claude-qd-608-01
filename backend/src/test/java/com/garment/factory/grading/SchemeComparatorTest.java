package com.garment.factory.grading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.garment.factory.grading.engine.DecimalQuantity;
import com.garment.factory.grading.engine.Direction;
import com.garment.factory.grading.engine.GradingGenerator;
import com.garment.factory.grading.engine.GradingPart;
import com.garment.factory.grading.engine.GradingSpec;
import com.garment.factory.grading.engine.LengthUnit;
import com.garment.factory.grading.engine.RuleConfig;
import com.garment.factory.grading.engine.RuleType;
import com.garment.factory.grading.engine.SchemeComparator;

/**
 * 两方案比较：公式差异 / 舍入差异 / 最终尺寸差异的区分。
 */
class SchemeComparatorTest {

    private static List<GradingSpec.SizeGrade> sizes() {
        return List.of(new GradingSpec.SizeGrade(-1, "S"),
                new GradingSpec.SizeGrade(0, "M"),
                new GradingSpec.SizeGrade(1, "L"));
    }

    private static GradingPart fixed(String id, String name, String base, String delta,
                                     LengthUnit unit, Direction dir) {
        RuleConfig rule = new RuleConfig();
        rule.setType(RuleType.FIXED);
        rule.setDeltaMicrons(DecimalQuantity.parseDelta(delta, unit));
        return new GradingPart(id, name, null, DecimalQuantity.parseLength(base, unit), dir, rule);
    }

    @Test
    @DisplayName("公式不同（增量 4cm vs 5cm）→ FORMULA")
    void formulaDifference() {
        var specA = new GradingSpec(sizes(), 0, new BigDecimal("1000"),
                List.of(fixed("bust", "胸围", "90", "4", LengthUnit.CM, Direction.UP)), List.of());
        var specB = new GradingSpec(sizes(), 0, new BigDecimal("1000"),
                List.of(fixed("bust", "胸围", "90", "5", LengthUnit.CM, Direction.UP)), List.of());
        var cmp = SchemeComparator.compare(GradingGenerator.generate(specA), GradingGenerator.generate(specB));

        var slot = cmp.slots().get(0);
        assertTrue(slot.formulaDifferent());
        assertTrue(slot.finalDifferent());
        var lCell = slot.cells().stream().filter(c -> c.grade() == 1).findFirst().orElseThrow();
        assertEquals(SchemeComparator.DiffKind.FORMULA, lCell.kind());
        assertTrue(cmp.counts().get("FORMULA") >= 1);
    }

    @Test
    @DisplayName("公式相同、未舍入相同，仅量子不同 → ROUNDING")
    void roundingDifferenceOnly() {
        // 4.05cm 增量：量子 1cm 时 L=94；量子 0.1cm 时 L=94.1。raw 相同（94.05）
        var specA = new GradingSpec(sizes(), 0, new BigDecimal("10000"), // 1cm
                List.of(fixed("bust", "胸围", "90", "4.05", LengthUnit.CM, Direction.UP)), List.of());
        var specB = new GradingSpec(sizes(), 0, new BigDecimal("1000"), // 0.1cm
                List.of(fixed("bust", "胸围", "90", "4.05", LengthUnit.CM, Direction.UP)), List.of());
        var cmp = SchemeComparator.compare(GradingGenerator.generate(specA), GradingGenerator.generate(specB));

        var slot = cmp.slots().get(0);
        assertFalse(slot.formulaDifferent(), "公式指纹应相同");
        assertTrue(slot.roundingDifferent(), "应识别为舍入差异");
        var lCell = slot.cells().stream().filter(c -> c.grade() == 1).findFirst().orElseThrow();
        assertEquals(SchemeComparator.DiffKind.ROUNDING, lCell.kind());
        assertEquals(0, lCell.rawA().compareTo(lCell.rawB()), "未舍入值必须相同");
        assertTrue(lCell.detail().contains("舍入口径不同"));
    }

    @Test
    @DisplayName("公式相同但基准尺寸不同 → FINAL（非公式、非舍入）")
    void finalDifferenceWithSameFormula() {
        var specA = new GradingSpec(sizes(), 0, new BigDecimal("1000"),
                List.of(fixed("bust", "胸围", "90", "4", LengthUnit.CM, Direction.UP)), List.of());
        var specB = new GradingSpec(sizes(), 0, new BigDecimal("1000"),
                List.of(fixed("bust", "胸围", "92", "4", LengthUnit.CM, Direction.UP)), List.of());
        var cmp = SchemeComparator.compare(GradingGenerator.generate(specA), GradingGenerator.generate(specB));

        var slot = cmp.slots().get(0);
        assertFalse(slot.formulaDifferent());
        assertTrue(slot.finalDifferent());
        var mCell = slot.cells().stream().filter(c -> c.grade() == 0).findFirst().orElseThrow();
        assertEquals(SchemeComparator.DiffKind.FINAL, mCell.kind());
    }

    @Test
    @DisplayName("两方案完全一致 → 无差异")
    void identical() {
        var specA = new GradingSpec(sizes(), 0, new BigDecimal("1000"),
                List.of(fixed("bust", "胸围", "90", "4", LengthUnit.CM, Direction.UP)), List.of());
        var specB = new GradingSpec(sizes(), 0, new BigDecimal("1000"),
                List.of(fixed("bust", "胸围", "90", "4", LengthUnit.CM, Direction.UP)), List.of());
        var cmp = SchemeComparator.compare(GradingGenerator.generate(specA), GradingGenerator.generate(specB));
        var slot = cmp.slots().get(0);
        assertFalse(slot.formulaDifferent());
        assertFalse(slot.finalDifferent());
        assertFalse(slot.roundingDifferent());
        assertTrue(slot.cells().stream().allMatch(c -> c.kind() == SchemeComparator.DiffKind.NONE));
    }
}
