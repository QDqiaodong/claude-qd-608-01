package com.garment.factory.dto;

import java.math.BigDecimal;

/**
 * 工单状态推进 / 回退入参：目标状态 + 实际工时 + 备注。
 */
public class WorkOrderAdvanceReq {

    private String targetStatus;
    private BigDecimal actualHours;
    private String remark;

    public String getTargetStatus() {
        return targetStatus;
    }

    public void setTargetStatus(String targetStatus) {
        this.targetStatus = targetStatus;
    }

    public BigDecimal getActualHours() {
        return actualHours;
    }

    public void setActualHours(BigDecimal actualHours) {
        this.actualHours = actualHours;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
