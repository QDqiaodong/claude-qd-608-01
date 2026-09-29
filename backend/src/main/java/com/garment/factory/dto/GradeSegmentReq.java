package com.garment.factory.dto;

import java.math.BigDecimal;

/**
 * 分段放码的一个码段：级序落在 [fromLevel, toLevel]（含端点）时，
 * 该段内每升一级累加 delta（delta 按方案录入单位给出，内部统一折成毫米）。
 * 级序以基准尺码为 0，升码为正、降码为负。
 */
public class GradeSegmentReq {

    private Integer fromLevel;
    private Integer toLevel;
    private BigDecimal delta;

    public Integer getFromLevel() {
        return fromLevel;
    }

    public void setFromLevel(Integer fromLevel) {
        this.fromLevel = fromLevel;
    }

    public Integer getToLevel() {
        return toLevel;
    }

    public void setToLevel(Integer toLevel) {
        this.toLevel = toLevel;
    }

    public BigDecimal getDelta() {
        return delta;
    }

    public void setDelta(BigDecimal delta) {
        this.delta = delta;
    }
}
