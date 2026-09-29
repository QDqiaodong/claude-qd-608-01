package com.garment.factory.grading;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.garment.factory.grading.doc.SchemeDoc;
import com.garment.factory.grading.doc.SchemeDocMapper;
import com.garment.factory.grading.engine.GradingGenerator;
import com.garment.factory.grading.engine.GradingResultSet;
import com.garment.factory.grading.engine.LengthUnit;

/**
 * 单位口径补充验收：同方案内 cm / mm 混录、含小数毫米输入。
 */
class UnitMixingTest {

    private final SchemeDocMapper mapper = new SchemeDocMapper();

    private SchemeDoc baseDoc() {
        SchemeDoc doc = new SchemeDoc();
        doc.setSchemaVersion(2);
        doc.setBaseGrade(0);
        doc.setInputUnit("cm");
        SchemeDoc.RoundingDoc r = new SchemeDoc.RoundingDoc();
        r.setUnit("cm");
        r.setStep("0.1");
        doc.setRounding(r);
        doc.setSizes(List.of(size(-1), size(0), size(1)));
        return doc;
    }

    private static SchemeDoc.SizeDoc size(int g) {
        SchemeDoc.SizeDoc s = new SchemeDoc.SizeDoc();
        s.setGrade(g);
        s.setName("G" + g);
        return s;
    }

    private static SchemeDoc.PartDoc part(String id, String name, String base, String baseUnit,
                                          String delta, String deltaUnit) {
        SchemeDoc.PartDoc p = new SchemeDoc.PartDoc();
        p.setPartId(id);
        p.setPartName(name);
        p.setBase(base);
        p.setUnit(baseUnit);
        p.setDirection("UP");
        SchemeDoc.RuleDoc rule = new SchemeDoc.RuleDoc();
        rule.setType("FIXED");
        rule.setDelta(delta);
        rule.setUnit(deltaUnit);
        p.setRule(rule);
        return p;
    }

    @Test
    @DisplayName("同方案内厘米与毫米混录：90cm+40mm 与 900mm+4cm 生成结果完全相同")
    void mixedUnitsProduceSameMicrons() {
        SchemeDoc cmDoc = baseDoc();
        cmDoc.setParts(List.of(
                part("bust", "胸围", "90", "cm", "4", "cm"),
                part("waist", "腰围", "900", "mm", "40", "mm")));

        SchemeDoc mmDoc = baseDoc();
        mmDoc.setParts(List.of(
                part("bust", "胸围", "900", "mm", "40", "mm"),
                part("waist", "腰围", "90", "cm", "4", "cm")));

        GradingResultSet a = GradingGenerator.generate(mapper.toSpec(cmDoc));
        GradingResultSet b = GradingGenerator.generate(mapper.toSpec(mmDoc));
        for (int g = -1; g <= 1; g++) {
            assertEquals(0, a.cell("bust", g).finalMicrons().compareTo(b.cell("bust", g).finalMicrons()));
            assertEquals(0, a.cell("waist", g).finalMicrons().compareTo(b.cell("waist", g).finalMicrons()));
        }
    }

    @Test
    @DisplayName("毫米允许一位小数：900.5mm 基准精确等于 90.05cm，内部不丢精度（最终再按量子舍入）")
    void fractionalMillimeterExact() {
        SchemeDoc doc = baseDoc();
        doc.setParts(List.of(part("x", "X", "900.5", "mm", "0", "mm")));
        GradingResultSet rs = GradingGenerator.generate(mapper.toSpec(doc));
        // raw 精确保留 900.5mm = 900500 μm = 90.05cm（输入精度在中间过程完整保留）
        assertEquals(0, rs.cell("x", 0).rawMicrons().compareTo(new BigDecimal("900500")));
        assertEquals("90.05", rs.cell("x", 0).rawMicrons()
                .divide(LengthUnit.CM.micronFactor()).toPlainString());
        // 0.1cm 量子统一舍入后为 90.1cm（901000μm）
        assertEquals(0, rs.cell("x", 0).finalMicrons().compareTo(new BigDecimal("901000")));

        // 切到 1mm 量子：0.5mm 同样按半进位到 1mm（901mm）
        doc.getRounding().setUnit("mm");
        doc.getRounding().setStep("1");
        GradingResultSet rsMm = GradingGenerator.generate(mapper.toSpec(doc));
        assertEquals(0, rsMm.cell("x", 0).finalMicrons().compareTo(new BigDecimal("901000")));
    }

    @Test
    @DisplayName("毫米模式录入往返：以 mm 建方案，结果 mm 字段与录入口径一致")
    void mmNativeScheme() {
        SchemeDoc doc = baseDoc();
        doc.setInputUnit("mm");
        doc.getRounding().setUnit("mm");
        doc.getRounding().setStep("1");
        doc.setParts(List.of(part("bust", "胸围", "900", "mm", "40", "mm")));
        GradingResultSet rs = GradingGenerator.generate(mapper.toSpec(doc));
        assertEquals(0, rs.cell("bust", 1).finalMicrons().compareTo(new BigDecimal("940000")));
        assertEquals(0, rs.quantumMicrons().stripTrailingZeros().compareTo(new BigDecimal("1000")));
    }
}
