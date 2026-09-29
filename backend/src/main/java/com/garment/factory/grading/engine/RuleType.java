package com.garment.factory.grading.engine;

import java.math.BigDecimal;

/**
 * 放码规则类型：
 * <ul>
 *   <li>{@link #FIXED} 固定增量：每升降一级变化量相同（可为负）；</li>
 *   <li>{@link #RATIO} 按比例变化：每级 = 基准 ×（1+比例）^级差（连乘，不逐段舍入）；</li>
 *   <li>{@link #SEGMENT} 分段规则：不同级差区间使用不同增量（如小码段 2cm、大码段 4cm）。</li>
 * </ul>
 */
public enum RuleType {
    FIXED,
    RATIO,
    SEGMENT;

    public static RuleType of(String code) {
        if (code == null) {
            throw new com.garment.factory.common.BizException("放码规则类型不能为空");
        }
        return switch (code.trim().toUpperCase()) {
            case "FIXED" -> FIXED;
            case "RATIO" -> RATIO;
            case "SEGMENT" -> SEGMENT;
            default -> throw new com.garment.factory.common.BizException(
                    "放码规则类型只能是 FIXED / RATIO / SEGMENT：" + code);
        };
    }

    /** 规则「公式指纹」：两方案比较时，公式相同才可能只有舍入差异。 */
    public String signature(RuleConfig rule) {
        return switch (this) {
            case FIXED -> "FIXED(" + strip(rule.getDeltaMicrons()) + ")";
            case RATIO -> "RATIO(" + rule.getRate().stripTrailingZeros().toPlainString() + ")";
            case SEGMENT -> {
                StringBuilder sb = new StringBuilder("SEGMENT(");
                for (Segment seg : rule.getSegments()) {
                    sb.append(seg.getFromGrade()).append("..").append(seg.getToGrade())
                            .append("=").append(strip(seg.getDeltaMicrons())).append(";");
                }
                sb.append(")");
                yield sb.toString();
            }
        };
    }

    private static String strip(BigDecimal v) {
        return v == null ? "null" : v.stripTrailingZeros().toPlainString();
    }
}
