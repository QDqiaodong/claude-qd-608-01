package com.garment.factory.dto;

import java.util.List;

/**
 * 两方案逐部位比较的一行（部位 + 侧别）。
 */
public class GradeCompareRow {

    private String part;
    private String side;
    private String ruleTypeA;
    private String ruleTypeB;
    private String formulaA;
    private String formulaB;
    /** 整行公式是否一致 */
    private boolean formulaSame;
    private List<GradeCompareCell> cells;
    /** 行级汇总标记：公式差异 / 仅舍入差异 / 最终尺寸差异的尺码数 */
    private int formulaDiffCount;
    private int roundingOnlyCount;
    private int valueDiffCount;

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

    public String getRuleTypeA() {
        return ruleTypeA;
    }

    public void setRuleTypeA(String ruleTypeA) {
        this.ruleTypeA = ruleTypeA;
    }

    public String getRuleTypeB() {
        return ruleTypeB;
    }

    public void setRuleTypeB(String ruleTypeB) {
        this.ruleTypeB = ruleTypeB;
    }

    public String getFormulaA() {
        return formulaA;
    }

    public void setFormulaA(String formulaA) {
        this.formulaA = formulaA;
    }

    public String getFormulaB() {
        return formulaB;
    }

    public void setFormulaB(String formulaB) {
        this.formulaB = formulaB;
    }

    public boolean isFormulaSame() {
        return formulaSame;
    }

    public void setFormulaSame(boolean formulaSame) {
        this.formulaSame = formulaSame;
    }

    public List<GradeCompareCell> getCells() {
        return cells;
    }

    public void setCells(List<GradeCompareCell> cells) {
        this.cells = cells;
    }

    public int getFormulaDiffCount() {
        return formulaDiffCount;
    }

    public void setFormulaDiffCount(int formulaDiffCount) {
        this.formulaDiffCount = formulaDiffCount;
    }

    public int getRoundingOnlyCount() {
        return roundingOnlyCount;
    }

    public void setRoundingOnlyCount(int roundingOnlyCount) {
        this.roundingOnlyCount = roundingOnlyCount;
    }

    public int getValueDiffCount() {
        return valueDiffCount;
    }

    public void setValueDiffCount(int valueDiffCount) {
        this.valueDiffCount = valueDiffCount;
    }
}
