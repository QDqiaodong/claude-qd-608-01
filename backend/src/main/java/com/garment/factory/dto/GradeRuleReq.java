package com.garment.factory.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 一条分部位放码规则。
 *   FIXED   固定增量：升一级累加 fixedDelta（可负，负值会被「增量方向错误」定位）
 *   RATIO   按比例：每级 = 基准 × ratioPercent%
 *   SEGMENT 分段：segments 给出各码段每级增量，小码段/大码段可不同
 * 长度参数均按方案录入单位给出，由服务端统一折毫米后再计算。
 */
public class GradeRuleReq {

    private String part;
    private String side;
    private String ruleType;
    private BigDecimal fixedDelta;
    private BigDecimal ratioPercent;
    private List<GradeSegmentReq> segments;

    public String getPart() {
        return part;
    }

    public void setPart(String part) {
        this.part = part;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public BigDecimal getFixedDelta() {
        return fixedDelta;
    }

    public void setFixedDelta(BigDecimal fixedDelta) {
        this.fixedDelta = fixedDelta;
    }

    public BigDecimal getRatioPercent() {
        return ratioPercent;
    }

    public void setRatioPercent(BigDecimal ratioPercent) {
        this.ratioPercent = ratioPercent;
    }

    public List<GradeSegmentReq> getSegments() {
        return segments;
    }

    public void setSegments(List<GradeSegmentReq> segments) {
        this.segments = segments;
    }
}
