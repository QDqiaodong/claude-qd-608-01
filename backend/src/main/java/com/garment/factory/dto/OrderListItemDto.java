package com.garment.factory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 定制订单列表行：由 JPQL 构造器表达式（JOIN 款式/面料/客户/量体）直接投影。
 */
public class OrderListItemDto {

    private final Long id;
    private final String orderNo;
    private final Long customerId;
    private final String customerName;
    private final String phone;
    private final String styleNo;
    private final String styleName;
    private final String category;
    private final String fabricNo;
    private final String fabricName;
    private final String fabricColor;
    private final String colorHex;
    private final Long measurementId;
    private final String recordNo;
    private final BigDecimal usageMeters;
    private final LocalDate orderDate;
    private final LocalDate deliveryDate;
    private final BigDecimal fabricCost;
    private final BigDecimal craftFee;
    private final BigDecimal totalPrice;
    private final BigDecimal deposit;
    private final BigDecimal balance;
    private final Boolean depositPaid;
    private final Boolean balancePaid;
    private final String status;
    private final String remark;

    public OrderListItemDto(Long id, String orderNo, Long customerId, String customerName, String phone,
                            String styleNo, String styleName, String category, String fabricNo, String fabricName,
                            String fabricColor, String colorHex, Long measurementId, String recordNo,
                            BigDecimal usageMeters, LocalDate orderDate, LocalDate deliveryDate,
                            BigDecimal fabricCost, BigDecimal craftFee, BigDecimal totalPrice,
                            BigDecimal deposit, BigDecimal balance, Boolean depositPaid, Boolean balancePaid,
                            String status, String remark) {
        this.id = id;
        this.orderNo = orderNo;
        this.customerId = customerId;
        this.customerName = customerName;
        this.phone = phone;
        this.styleNo = styleNo;
        this.styleName = styleName;
        this.category = category;
        this.fabricNo = fabricNo;
        this.fabricName = fabricName;
        this.fabricColor = fabricColor;
        this.colorHex = colorHex;
        this.measurementId = measurementId;
        this.recordNo = recordNo;
        this.usageMeters = usageMeters;
        this.orderDate = orderDate;
        this.deliveryDate = deliveryDate;
        this.fabricCost = fabricCost;
        this.craftFee = craftFee;
        this.totalPrice = totalPrice;
        this.deposit = deposit;
        this.balance = balance;
        this.depositPaid = depositPaid;
        this.balancePaid = balancePaid;
        this.status = status;
        this.remark = remark;
    }

    public Long getId() {
        return id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getPhone() {
        return phone;
    }

    public String getStyleNo() {
        return styleNo;
    }

    public String getStyleName() {
        return styleName;
    }

    public String getCategory() {
        return category;
    }

    public String getFabricNo() {
        return fabricNo;
    }

    public String getFabricName() {
        return fabricName;
    }

    public String getFabricColor() {
        return fabricColor;
    }

    public String getColorHex() {
        return colorHex;
    }

    public Long getMeasurementId() {
        return measurementId;
    }

    public String getRecordNo() {
        return recordNo;
    }

    public BigDecimal getUsageMeters() {
        return usageMeters;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public LocalDate getDeliveryDate() {
        return deliveryDate;
    }

    public BigDecimal getFabricCost() {
        return fabricCost;
    }

    public BigDecimal getCraftFee() {
        return craftFee;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public BigDecimal getDeposit() {
        return deposit;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public Boolean getDepositPaid() {
        return depositPaid;
    }

    public Boolean getBalancePaid() {
        return balancePaid;
    }

    public String getStatus() {
        return status;
    }

    public String getRemark() {
        return remark;
    }
}
