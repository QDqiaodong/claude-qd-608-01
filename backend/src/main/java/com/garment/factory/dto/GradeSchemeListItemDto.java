package com.garment.factory.dto;

import java.time.LocalDateTime;

/**
 * 放码方案列表项：只投影头部信息与异常计数，不展开快照。
 */
public class GradeSchemeListItemDto {

    private Long id;
    private String schemeNo;
    private String name;
    private Long styleId;
    private String styleName;
    private String baseSizeCode;
    private String inputUnit;
    private Integer sizeCount;
    private Integer partCount;
    private Integer issueCount;
    private Boolean legacyFlag;
    private Integer resultVersion;
    private LocalDateTime updatedAt;

    public GradeSchemeListItemDto() {
    }

    public GradeSchemeListItemDto(Long id, String schemeNo, String name, Long styleId, String styleName,
                                  String baseSizeCode, String inputUnit, Integer sizeCount, Integer partCount,
                                  Integer issueCount, Boolean legacyFlag, Integer resultVersion,
                                  LocalDateTime updatedAt) {
        this.id = id;
        this.schemeNo = schemeNo;
        this.name = name;
        this.styleId = styleId;
        this.styleName = styleName;
        this.baseSizeCode = baseSizeCode;
        this.inputUnit = inputUnit;
        this.sizeCount = sizeCount;
        this.partCount = partCount;
        this.issueCount = issueCount;
        this.legacyFlag = legacyFlag;
        this.resultVersion = resultVersion;
        this.updatedAt = updatedAt;
    }

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

    public String getStyleName() {
        return styleName;
    }

    public void setStyleName(String styleName) {
        this.styleName = styleName;
    }

    public String getBaseSizeCode() {
        return baseSizeCode;
    }

    public void setBaseSizeCode(String baseSizeCode) {
        this.baseSizeCode = baseSizeCode;
    }

    public String getInputUnit() {
        return inputUnit;
    }

    public void setInputUnit(String inputUnit) {
        this.inputUnit = inputUnit;
    }

    public Integer getSizeCount() {
        return sizeCount;
    }

    public void setSizeCount(Integer sizeCount) {
        this.sizeCount = sizeCount;
    }

    public Integer getPartCount() {
        return partCount;
    }

    public void setPartCount(Integer partCount) {
        this.partCount = partCount;
    }

    public Integer getIssueCount() {
        return issueCount;
    }

    public void setIssueCount(Integer issueCount) {
        this.issueCount = issueCount;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
