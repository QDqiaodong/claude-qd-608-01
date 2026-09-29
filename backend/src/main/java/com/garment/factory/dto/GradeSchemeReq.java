package com.garment.factory.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 放码方案计算/留存请求。
 *
 * 版师可以：
 *   - 绑定现有款式（styleId）或建立临时计算方案（styleId 为空，不落库只试算）；
 *   - 用 inputUnit 选择厘米/毫米录入；roundStepMm 指定统一舍入步进（缺省 1 毫米）。
 * 服务端先把全部长度折成毫米、校验通过后再一次性生成整组结果：
 * 任一规则无法计算都会整体失败，不会产生半套结果。
 */
public class GradeSchemeReq {

    private Long id;
    private String schemeNo;
    private String name;
    private Long styleId;
    private String inputUnit;
    private BigDecimal roundStepMm;
    private String baseSizeCode;
    /** 目标尺码序列，按从小到大的顺序给出 */
    private List<String> sizes;
    private List<GradeBaseReq> bases;
    private List<GradeRuleReq> rules;
    /** 旧数据兼容标记，仅由迁移/种子数据置位 */
    private Boolean legacyFlag;

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
}
