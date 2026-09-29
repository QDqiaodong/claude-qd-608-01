package com.garment.factory.service.grade;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 规范化后的方案：内部唯一事实来源。
 *   inputUnit 只服务回显；一切长度均为毫米（BigDecimal），
 *   levels 与 sizes 一一对应，基准码级序为 0。
 */
public class CanonicalScheme {

    private String inputUnit;
    private BigDecimal roundStepMm;
    private List<String> sizes;
    private List<Integer> levels;
    private String baseSizeCode;
    private int baseIndex;
    private Map<PartSide, BigDecimal> bases;
    private Map<PartSide, NormRule> rules;
    private boolean legacy;

    public CanonicalScheme() {
    }

    public String getInputUnit() {
        return inputUnit;
    }

    public void setInputUnit(String inputUnit) {
        this.inputUnit = inputUnit;
    }

    public BigDecimal getRoundStepMm() {
        return roundStepMm;
    }

    public void setRoundStepMm(BigDecimal roundStepMm) {
        this.roundStepMm = roundStepMm;
    }

    public List<String> getSizes() {
        return sizes;
    }

    public void setSizes(List<String> sizes) {
        this.sizes = sizes;
    }

    public List<Integer> getLevels() {
        return levels;
    }

    public void setLevels(List<Integer> levels) {
        this.levels = levels;
    }

    public String getBaseSizeCode() {
        return baseSizeCode;
    }

    public void setBaseSizeCode(String baseSizeCode) {
        this.baseSizeCode = baseSizeCode;
    }

    public int getBaseIndex() {
        return baseIndex;
    }

    public void setBaseIndex(int baseIndex) {
        this.baseIndex = baseIndex;
    }

    public Map<PartSide, BigDecimal> getBases() {
        return bases;
    }

    public void setBases(Map<PartSide, BigDecimal> bases) {
        this.bases = bases;
    }

    public Map<PartSide, NormRule> getRules() {
        return rules;
    }

    public void setRules(Map<PartSide, NormRule> rules) {
        this.rules = rules;
    }

    public boolean isLegacy() {
        return legacy;
    }

    public void setLegacy(boolean legacy) {
        this.legacy = legacy;
    }
}
