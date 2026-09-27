package com.garment.factory.dto;

import java.math.BigDecimal;

/**
 * 工单列表项：工单页表格 / 看板抽屉用（JPQL 构造器表达式投影）。
 */
public class WorkOrderListItemDto {

    private final Long id;
    private final String woNo;
    private final Long orderId;
    private final String orderNo;
    private final String customerName;
    private final String styleName;
    private final String step;
    private final String owner;
    private final BigDecimal plannedHours;
    private final BigDecimal actualHours;
    private final String status;
    private final String remark;

    public WorkOrderListItemDto(Long id, String woNo, Long orderId, String orderNo, String customerName,
                                String styleName, String step, String owner, BigDecimal plannedHours,
                                BigDecimal actualHours, String status, String remark) {
        this.id = id;
        this.woNo = woNo;
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.customerName = customerName;
        this.styleName = styleName;
        this.step = step;
        this.owner = owner;
        this.plannedHours = plannedHours;
        this.actualHours = actualHours;
        this.status = status;
        this.remark = remark;
    }

    public Long getId() {
        return id;
    }

    public String getWoNo() {
        return woNo;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getStyleName() {
        return styleName;
    }

    public String getStep() {
        return step;
    }

    public String getOwner() {
        return owner;
    }

    public BigDecimal getPlannedHours() {
        return plannedHours;
    }

    public BigDecimal getActualHours() {
        return actualHours;
    }

    public String getStatus() {
        return status;
    }

    public String getRemark() {
        return remark;
    }
}
