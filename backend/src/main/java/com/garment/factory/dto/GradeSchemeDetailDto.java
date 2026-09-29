package com.garment.factory.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 方案详情：回显用的录入参数（已换算回当初的录入单位）+ 服务端留存的完整结果快照。
 * 重新打开只读快照，不做任何重新舍入，保证与留存时完全一致。
 */
public class GradeSchemeDetailDto {

    private Long id;
    private String schemeNo;
    private String name;
    private Long styleId;
    private String inputUnit;
    private BigDecimal roundStepMm;
    private String baseSizeCode;
    private List<String> sizes;
    private List<GradeBaseReq> bases;
    private List<GradeRuleReq> rules;
    private Boolean legacyFlag;
    private Integer resultVersion;
    private GradeResult result;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSchemeNo() {
        return schemeNo;
    }

    public void setSchemeNo(String schemeNo) {
        this.schemeNo = schemeNo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getStyleId() {
        return styleId;
    }

    public void setStyleId(Long styleId) {
        this.styleId = styleId;
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

    public String getBaseSizeCode() {
        return baseSizeCode;
    }

    public void setBaseSizeCode(String baseSizeCode) {
        this.baseSizeCode = baseSizeCode;
    }

    public List<String> getSizes() {
        return sizes;
    }

    public void setSizes(List<String> sizes) {
        this.sizes = sizes;
    }

    public List<GradeBaseReq> getBases() {
        return bases;
    }

    public void setBases(List<GradeBaseReq> bases) {
        this.bases = bases;
    }

    public List<GradeRuleReq> getRules() {
        return rules;
    }

    public void setRules(List<GradeRuleReq> rules) {
        this.rules = rules;
    }

    public Boolean getLegacyFlag() {
        return legacyFlag;
    }

    public void setLegacyFlag(Boolean legacyFlag) {
        this.legacyFlag = legacyFlag;
    }

    public Integer getResultVersion() {
        return resultVersion;
    }

    public void setResultVersion(Integer resultVersion) {
        this.resultVersion = resultVersion;
    }

    public GradeResult getResult() {
        return result;
    }

    public void setResult(GradeResult result) {
        this.result = result;
    }
}
