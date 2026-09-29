package com.garment.factory.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 一次完整放码生成的结果（也是落库快照的结构）。
 *
 * 整组结果一次性算完后整体返回/落库；计算中任一公式无法计算会整体抛错，
 * 由 service 保证旧快照原样保留，不残留半套新结果。
 */
public class GradeResult {

    private String baseSizeCode;
    private Integer baseLevel;
    private String inputUnit;
    /** 舍入步进（毫米） */
    private BigDecimal roundStepMm;
    /** 有序尺码序列 */
    private List<String> sizes;
    private List<Integer> levels;
    private List<GradePartResult> parts;
    /** 异常关系定位（非致命：结果照样给出，但要请版师核对） */
    private List<GradeIssue> issues;
    /** 批量计算性能（毫秒） */
    private long elapsedMs;

    public String getBaseSizeCode() {
        return baseSizeCode;
    }

    public void setBaseSizeCode(String baseSizeCode) {
        this.baseSizeCode = baseSizeCode;
    }

    public Integer getBaseLevel() {
        return baseLevel;
    }

    public void setBaseLevel(Integer baseLevel) {
        this.baseLevel = baseLevel;
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

    public List<GradePartResult> getParts() {
        return parts;
    }

    public void setParts(List<GradePartResult> parts) {
        this.parts = parts;
    }

    public List<GradeIssue> getIssues() {
        return issues;
    }

    public void setIssues(List<GradeIssue> issues) {
        this.issues = issues;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }

    public void setElapsedMs(long elapsedMs) {
        this.elapsedMs = elapsedMs;
    }
}
