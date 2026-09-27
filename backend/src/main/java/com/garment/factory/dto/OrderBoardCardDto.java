package com.garment.factory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * 订单进度看板卡片：列表投影用；工序完成情况在 service 里另行汇总填充。
 */
public class OrderBoardCardDto {

    private final Long id;
    private final String orderNo;
    private final String customerName;
    private final String styleName;
    private final String fabricName;
    private final String colorHex;
    private final String tailorName;
    private final LocalDate deliveryDate;
    private final BigDecimal totalPrice;
    private final BigDecimal usageMeters;
    private final String status;
    private final Boolean balancePaid;

    /** 工单总数（服务层回填） */
    private long workOrderTotal;
    /** 已完成工单数（服务层回填） */
    private long workOrderDone;

    /** 试衣整改汇总（服务层回填：未关闭数/阻断数/返修轮次/当前阶段） */
    private Map<String, Object> fitting;

    public OrderBoardCardDto(Long id, String orderNo, String customerName, String styleName, String fabricName,
                             String colorHex, String tailorName, LocalDate deliveryDate, BigDecimal totalPrice,
                             BigDecimal usageMeters, String status, Boolean balancePaid) {
        this.id = id;
        this.orderNo = orderNo;
        this.customerName = customerName;
        this.styleName = styleName;
        this.fabricName = fabricName;
        this.colorHex = colorHex;
        this.tailorName = tailorName;
        this.deliveryDate = deliveryDate;
        this.totalPrice = totalPrice;
        this.usageMeters = usageMeters;
        this.status = status;
        this.balancePaid = balancePaid;
    }

    public Long getId() {
        return id;
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

    public String getFabricName() {
        return fabricName;
    }

    public String getColorHex() {
        return colorHex;
    }

    public String getTailorName() {
        return tailorName;
    }

    public LocalDate getDeliveryDate() {
        return deliveryDate;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public BigDecimal getUsageMeters() {
        return usageMeters;
    }

    public String getStatus() {
        return status;
    }

    public Boolean getBalancePaid() {
        return balancePaid;
    }

    public long getWorkOrderTotal() {
        return workOrderTotal;
    }

    public void setWorkOrderTotal(long workOrderTotal) {
        this.workOrderTotal = workOrderTotal;
    }

    public long getWorkOrderDone() {
        return workOrderDone;
    }

    public void setWorkOrderDone(long workOrderDone) {
        this.workOrderDone = workOrderDone;
    }

    public Map<String, Object> getFitting() {
        return fitting;
    }

    public void setFitting(Map<String, Object> fitting) {
        this.fitting = fitting;
    }
}
