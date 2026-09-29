package com.garment.factory.dto;

import java.util.List;

/**
 * 一条部位（+侧别）的整组放码结果：cells 与尺码序列一一对应。
 * formula 是该规则的可读公式，供前端展示与两方案比较。
 */
public class GradePartResult {

    private String part;
    private String side;
    private String ruleType;
    private String formula;
    private List<GradeCell> cells;

    public GradePartResult() {
    }

    public GradePartResult(String part, String side, String ruleType, String formula, List<GradeCell> cells) {
        this.part = part;
        this.side = side;
        this.ruleType = ruleType;
        this.formula = formula;
        this.cells = cells;
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

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public String getFormula() {
        return formula;
    }

    public void setFormula(String formula) {
        this.formula = formula;
    }

    public List<GradeCell> getCells() {
        return cells;
    }

    public void setCells(List<GradeCell> cells) {
        this.cells = cells;
    }
}
