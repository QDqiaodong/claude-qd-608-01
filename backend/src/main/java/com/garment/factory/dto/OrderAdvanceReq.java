package com.garment.factory.dto;

/**
 * 订单状态推进入参。
 * 从「试衣中」推进到「已完成」时，若存在未关闭的普通问题，必须由操作人显式确认放行并写明原因。
 */
public class OrderAdvanceReq {

    private String targetStatus;
    /** 操作人（完工收口、普通问题放行留痕用） */
    private String operator;
    /** true = 已知悉未关闭的普通问题并确认随单放行 */
    private Boolean confirmRelease;
    /** 普通问题放行确认说明（confirmRelease=true 时必填） */
    private String releaseReason;

    public String getTargetStatus() {
        return targetStatus;
    }

    public void setTargetStatus(String targetStatus) {
        this.targetStatus = targetStatus;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Boolean getConfirmRelease() {
        return confirmRelease;
    }

    public void setConfirmRelease(Boolean confirmRelease) {
        this.confirmRelease = confirmRelease;
    }

    public String getReleaseReason() {
        return releaseReason;
    }

    public void setReleaseReason(String releaseReason) {
        this.releaseReason = releaseReason;
    }
}
