package com.garment.factory.dto;

import java.util.List;

/**
 * 两套放码方案的逐部位比较结果：公式差异 / 舍入差异 / 最终尺寸差异三类分开统计。
 */
public class GradeCompareResult {

    private Long schemeIdA;
    private String schemeNameA;
    private Long schemeIdB;
    private String schemeNameB;
    /** 两尺码序列的交集，逐格比较只在交集尺码上进行 */
    private List<String> commonSizes;
    private List<GradeCompareRow> rows;
    private int formulaDiffCells;
    private int roundingOnlyCells;
    private int valueDiffCells;
    private int identicalCells;
    private int totalCells;

    public Long getSchemeIdA() {
        return schemeIdA;
    }

    public void setSchemeIdA(Long schemeIdA) {
        this.schemeIdA = schemeIdA;
    }

    public String getSchemeNameA() {
        return schemeNameA;
    }

    public void setSchemeNameA(String schemeNameA) {
        this.schemeNameA = schemeNameA;
    }

    public Long getSchemeIdB() {
        return schemeIdB;
    }

    public void setSchemeIdB(Long schemeIdB) {
        this.schemeIdB = schemeIdB;
    }

    public String getSchemeNameB() {
        return schemeNameB;
    }

    public void setSchemeNameB(String schemeNameB) {
        this.schemeNameB = schemeNameB;
    }

    public List<String> getCommonSizes() {
        return commonSizes;
    }

    public void setCommonSizes(List<String> commonSizes) {
        this.commonSizes = commonSizes;
    }

    public List<GradeCompareRow> getRows() {
        return rows;
    }

    public void setRows(List<GradeCompareRow> rows) {
        this.rows = rows;
    }

    public int getFormulaDiffCells() {
        return formulaDiffCells;
    }

    public void setFormulaDiffCells(int formulaDiffCells) {
        this.formulaDiffCells = formulaDiffCells;
    }

    public int getRoundingOnlyCells() {
        return roundingOnlyCells;
    }

    public void setRoundingOnlyCells(int roundingOnlyCells) {
        this.roundingOnlyCells = roundingOnlyCells;
    }

    public int getValueDiffCells() {
        return valueDiffCells;
    }

    public void setValueDiffCells(int valueDiffCells) {
        this.valueDiffCells = valueDiffCells;
    }

    public int getIdenticalCells() {
        return identicalCells;
    }

    public void setIdenticalCells(int identicalCells) {
        this.identicalCells = identicalCells;
    }

    public int getTotalCells() {
        return totalCells;
    }

    public void setTotalCells(int totalCells) {
        this.totalCells = totalCells;
    }
}
