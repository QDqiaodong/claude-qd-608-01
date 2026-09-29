package com.garment.factory.dto;

import java.math.BigDecimal;

/**
 * 放码结果中的一个尺码值。
 *   rawMm 舍入前的毫米理论值（差异比较时区分「公式差异/舍入差异」用）
 *   valueMm 统一舍入后的最终毫米值（唯一规范结果，厘米值 = valueMm/10）
 *   deltaMm 相对基准版（舍入后基准）的总变化
 */
public class GradeCell {

    private String sizeCode;
    private Integer level;
    private BigDecimal rawMm;
    private BigDecimal valueMm;
    private BigDecimal valueCm;
    private BigDecimal deltaMm;

    public GradeCell() {
    }

    public GradeCell(String sizeCode, Integer level, BigDecimal rawMm,
                     BigDecimal valueMm, BigDecimal valueCm, BigDecimal deltaMm) {
        this.sizeCode = sizeCode;
        this.level = level;
        this.rawMm = rawMm;
        this.valueMm = valueMm;
        this.valueCm = valueCm;
        this.deltaMm = deltaMm;
    }

    public String getSizeCode() {
        return sizeCode;
    }

    public void setSizeCode(String sizeCode) {
        this.sizeCode = sizeCode;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public BigDecimal getRawMm() {
        return rawMm;
    }

    public void setRawMm(BigDecimal rawMm) {
        this.rawMm = rawMm;
    }

    public BigDecimal getValueMm() {
        return valueMm;
    }

    public void setValueMm(BigDecimal valueMm) {
        this.valueMm = valueMm;
    }

    public BigDecimal getValueCm() {
        return valueCm;
    }

    public void setValueCm(BigDecimal valueCm) {
        this.valueCm = valueCm;
    }

    public BigDecimal getDeltaMm() {
        return deltaMm;
    }

    public void setDeltaMm(BigDecimal deltaMm) {
        this.deltaMm = deltaMm;
    }
}
