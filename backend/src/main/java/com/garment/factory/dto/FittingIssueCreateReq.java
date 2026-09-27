package com.garment.factory.dto;

/**
 * 建立试衣问题单入参：只有处于「试衣中」的订单可以建单。
 */
public class FittingIssueCreateReq {

    private Long orderId;
    private String bodyPart;
    /** MINOR 普通问题 / BLOCKER 阻断交付 */
    private String severity;
    private String description;
    /** 责任工序（阻断交付问题必须关联） */
    private String responsibleStep;
    private String raisedBy;
    private String remark;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getBodyPart() {
        return bodyPart;
    }

    public void setBodyPart(String bodyPart) {
        this.bodyPart = bodyPart;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getResponsibleStep() {
        return responsibleStep;
    }

    public void setResponsibleStep(String responsibleStep) {
        this.responsibleStep = responsibleStep;
    }

    public String getRaisedBy() {
        return raisedBy;
    }

    public void setRaisedBy(String raisedBy) {
        this.raisedBy = raisedBy;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
