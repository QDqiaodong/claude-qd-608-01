package com.garment.factory.dto;

import java.math.BigDecimal;

/**
 * 某部位某一侧的基准尺寸（按方案录入单位给出：厘米或毫米）。
 * side 缺省为 CENTER；肩宽/袖长可用 LEFT/RIGHT 分别录入，系统不会自动合并。
 */
public class GradeBaseReq {

    private String part;
    private String side;
    private BigDecimal value;

    public GradeBaseReq() {
    }

    public GradeBaseReq(String part, String side, BigDecimal value) {
        this.part = part;
        this.side = side;
        this.value = value;
    }

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

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }
}
