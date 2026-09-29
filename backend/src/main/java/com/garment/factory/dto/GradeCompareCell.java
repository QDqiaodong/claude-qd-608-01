package com.garment.factory.dto;

import java.math.BigDecimal;

/**
 * 两方案逐部位比较的一个尺码单元。
 * tags 同时给出差异分类（可并存）：
 *   FORMULA 公式差异（规则类型或参数不同，附两套公式）
 *   ROUNDING 公式相同，差异仅来自统一舍入
 *   VALUE    最终尺寸不同（无论成因）
 *   MISSING  该部位/侧别只存在于其中一套方案
 */
public class GradeCompareCell {

    private String sizeCode;
    private Integer level;
    private BigDecimal valueMmA;
    private BigDecimal valueMmB;
    private BigDecimal diffMm;
    private Boolean formulaSame;
    private java.util.List<String> tags;

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

    public BigDecimal getValueMmA() {
        return valueMmA;
    }

    public void setValueMmA(BigDecimal valueMmA) {
        this.valueMmA = valueMmA;
    }

    public BigDecimal getValueMmB() {
        return valueMmB;
    }

    public void setValueMmB(BigDecimal valueMmB) {
        this.valueMmB = valueMmB;
    }

    public BigDecimal getDiffMm() {
        return diffMm;
    }

    public void setDiffMm(BigDecimal diffMm) {
        this.diffMm = diffMm;
    }

    public Boolean getFormulaSame() {
        return formulaSame;
    }

    public void setFormulaSame(Boolean formulaSame) {
        this.formulaSame = formulaSame;
    }

    public java.util.List<String> getTags() {
        return tags;
    }

    public void setTags(java.util.List<String> tags) {
        this.tags = tags;
    }
}
