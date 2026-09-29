package com.garment.factory.grading.engine;

import java.math.BigDecimal;
import java.util.List;

/**
 * 一份「放码计算方案」的引擎输入（与存储 / 传输 DTO 解耦）。
 *
 * <p>不变量（由 {@code GradingSpecBuilder} 负责）：
 * <ol>
 *   <li>尺码按 {@code grade} 升序、级差唯一且为连续或指定整数，基准级差在尺码序列内；</li>
 *   <li>所有长度均为微米有理数量；</li>
 *   <li>分段规则的边完整覆盖 [最低级差, 最高级差) 且互不重叠。</li>
 * </ol>
 */
public class GradingSpec {

    /** 尺码（级差 + 名称），按级差升序。 */
    public record SizeGrade(int grade, String name) {
    }

    private final List<SizeGrade> sizes;
    private final int baseGrade;
    private final BigDecimal quantumMicrons;
    private final List<GradingPart> parts;
    /** 比例关系检查配置（如肩宽/胸围）。 */
    private final List<RatioCheck> ratioChecks;

    public GradingSpec(List<SizeGrade> sizes, int baseGrade, BigDecimal quantumMicrons,
                       List<GradingPart> parts, List<RatioCheck> ratioChecks) {
        this.sizes = sizes;
        this.baseGrade = baseGrade;
        this.quantumMicrons = quantumMicrons;
        this.parts = parts;
        this.ratioChecks = ratioChecks == null ? List.of() : ratioChecks;
    }

    public List<SizeGrade> sizes() {
        return sizes;
    }

    public int baseGrade() {
        return baseGrade;
    }

    public BigDecimal quantumMicrons() {
        return quantumMicrons;
    }

    public List<GradingPart> parts() {
        return parts;
    }

    public List<RatioCheck> ratioChecks() {
        return ratioChecks;
    }

    public SizeGrade baseSize() {
        return sizes.stream().filter(s -> s.grade() == baseGrade).findFirst().orElseThrow();
    }

    public int minGrade() {
        return sizes.get(0).grade();
    }

    public int maxGrade() {
        return sizes.get(sizes.size() - 1).grade();
    }

    public String sizeName(int grade) {
        return sizes.stream().filter(s -> s.grade() == grade).map(SizeGrade::name).findFirst().orElse("级差" + grade);
    }
}
