package com.garment.factory.service.grade;

import java.math.BigDecimal;
import java.util.List;

import com.garment.factory.domain.GradeDict;

/**
 * 规范化后的一条放码规则（全部长度参数已是毫米）。
 * ruleType 为 null/空表示该部位没有规则：所有尺码保持基准值（公式：基准不变）。
 */
public record NormRule(String part, String side, String ruleType,
                       BigDecimal fixedDeltaMm, BigDecimal ratioPercent,
                       List<NormSegment> segments) {

    public PartSide key() {
        return new PartSide(part, side);
    }

    public boolean isConstant() {
        return ruleType == null || ruleType.isBlank();
    }

    public String type() {
        return ruleType == null ? "CONST" : ruleType;
    }

    public static NormRule constant(String part, String side) {
        return new NormRule(part, side, GradeDict.RULE_FIXED, BigDecimal.ZERO, null, List.of());
    }
}
