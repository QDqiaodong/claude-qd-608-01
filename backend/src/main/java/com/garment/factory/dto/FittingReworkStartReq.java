package com.garment.factory.dto;

import java.math.BigDecimal;

/**
 * 发起返修入参：每次发起都生成独立返修轮次，原工序工单保持不动。
 * 责任人/计划工时/返修原因必填；复核不通过开启下一轮时也走这套入参。
 */
public class FittingReworkStartReq {

    private String operator;
    private String owner;
    private BigDecimal plannedHours;
    private String reworkReason;
    /** 前端页面版本号（可空）：与服务端不一致时直接拒绝，提示刷新 */
    private Integer expectedVersion;

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
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

    public String getReworkReason() {
        return reworkReason;
    }

    public void setReworkReason(String reworkReason) {
        this.reworkReason = reworkReason;
    }

    public Integer getExpectedVersion() {
        return expectedVersion;
    }

    public void setExpectedVersion(Integer expectedVersion) {
        this.expectedVersion = expectedVersion;
    }
}
