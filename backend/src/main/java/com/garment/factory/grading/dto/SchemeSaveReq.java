package com.garment.factory.grading.dto;

import com.garment.factory.grading.doc.SchemeDoc;

/**
 * 放码方案保存请求。
 */
public class SchemeSaveReq {

    private Long id;
    private String schemeNo;
    private String name;
    private Long styleId;
    private Boolean temporary;
    private String remark;
    private SchemeDoc spec;

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

    public Boolean getTemporary() {
        return temporary;
    }

    public void setTemporary(Boolean temporary) {
        this.temporary = temporary;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public SchemeDoc getSpec() {
        return spec;
    }

    public void setSpec(SchemeDoc spec) {
        this.spec = spec;
    }
}
