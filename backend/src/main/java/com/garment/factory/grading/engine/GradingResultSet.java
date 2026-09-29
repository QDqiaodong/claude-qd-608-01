package com.garment.factory.grading.engine;

import java.math.BigDecimal;
import java.util.List;

/**
 * 一次整组放码生成的完整结果：全尺码 × 全槽位 + 定位异常清单。
 * 结构错误时不会产生该对象（引擎抛异常），保证「不留下半套新结果」。
 */
public class GradingResultSet {

    private final BigDecimal quantumMicrons;
    private final int baseGrade;
    private final List<GradingSpec.SizeGrade> sizes;
    private final List<GradingPart> parts;
    private final List<GradingCell> cells;
    private final List<GradingAnomaly> anomalies;
    /** 规则公式指纹（槽位 key → 指纹），供两方案比较。 */
    private final java.util.Map<String, String> formulaSignatures;

    /** 本次计算耗时（微秒），仅展示用，不参与数值与指纹。 */
    public transient long elapsedMicrosForDisplay;

    public GradingResultSet(BigDecimal quantumMicrons, int baseGrade,
                            List<GradingSpec.SizeGrade> sizes, List<GradingPart> parts,
                            List<GradingCell> cells, List<GradingAnomaly> anomalies,
                            java.util.Map<String, String> formulaSignatures) {
        this.quantumMicrons = quantumMicrons;
        this.baseGrade = baseGrade;
        this.sizes = sizes;
        this.parts = parts;
        this.cells = cells;
        this.anomalies = anomalies;
        this.formulaSignatures = formulaSignatures;
    }

    public BigDecimal quantumMicrons() {
        return quantumMicrons;
    }

    public int baseGrade() {
        return baseGrade;
    }

    public List<GradingSpec.SizeGrade> sizes() {
        return sizes;
    }

    public List<GradingPart> parts() {
        return parts;
    }

    public List<GradingCell> cells() {
        return cells;
    }

    public List<GradingAnomaly> anomalies() {
        return anomalies;
    }

    public java.util.Map<String, String> formulaSignatures() {
        return formulaSignatures;
    }

    public GradingCell cell(String slotKey, int grade) {
        return cells.stream()
                .filter(c -> c.slotKey().equals(slotKey) && c.grade() == grade)
                .findFirst()
                .orElse(null);
    }
}
