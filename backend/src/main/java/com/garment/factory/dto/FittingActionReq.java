package com.garment.factory.dto;

import java.math.BigDecimal;

/**
 * 整改台动作统一入参：返修完成 / 提交复核 / 复核通过 / 复核不通过 / 重新打开 / 普通问题放行。
 * 不同动作只取自己关心的字段，服务端按动作分别校验必填项。
 */
public class FittingActionReq {

    /** 操作人（登记工时的返修人 / 复核人 / 重开人 / 放行人） */
    private String operator;
    /** 返修完成时登记的实际工时，必须大于 0 */
    private BigDecimal actualHours;
    /** 复核不通过原因 / 重新打开原因 / 放行确认说明 */
    private String reason;
    /** 重新打开时若需调整责任工序可覆盖（默认沿用原责任工序） */
    private String responsibleStep;
    /** 重新打开时指定返修负责人（重开必须同时发起新一轮返修） */
    private String owner;
    /** 重新开新一轮返修的计划工时 */
    private BigDecimal plannedHours;
    /** 前端页面版本号（可空）：过期页面的提交直接拒绝并回传真实状态 */
    private Integer expectedVersion;

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public BigDecimal getActualHours() {
        return actualHours;
    }

    public void setActualHours(BigDecimal actualHours) {
        this.actualHours = actualHours;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getResponsibleStep() {
        return responsibleStep;
    }

    public void setResponsibleStep(String responsibleStep) {
        this.responsibleStep = responsibleStep;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public BigDecimal getPlannedHours() {
        return plannedHours;
    }

    public void setPlannedHours(BigDecimal plannedHours) {
        this.plannedHours = plannedHours;
    }

    public Integer getExpectedVersion() {
        return expectedVersion;
    }

    public void setExpectedVersion(Integer expectedVersion) {
        this.expectedVersion = expectedVersion;
    }
}
