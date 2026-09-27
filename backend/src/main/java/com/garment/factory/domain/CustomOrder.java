package com.garment.factory.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 定制订单：客户 + 款式 + 面料 + 量体记录，定金 50%、尾款 50%。
 */
@Entity
@Table(name = "custom_order")
public class CustomOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, length = 30)
    private String orderNo;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "style_id", nullable = false)
    private Long styleId;

    @Column(name = "fabric_id", nullable = false)
    private Long fabricId;

    @Column(name = "measurement_id")
    private Long measurementId;

    @Column(name = "usage_meters", nullable = false)
    private BigDecimal usageMeters;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;

    @Column(name = "fabric_cost", nullable = false)
    private BigDecimal fabricCost;

    @Column(name = "craft_fee", nullable = false)
    private BigDecimal craftFee;

    @Column(name = "total_price", nullable = false)
    private BigDecimal totalPrice;

    @Column(name = "deposit", nullable = false)
    private BigDecimal deposit;

    @Column(name = "balance", nullable = false)
    private BigDecimal balance;

    @Column(name = "deposit_paid", nullable = false)
    private Boolean depositPaid;

    @Column(name = "balance_paid", nullable = false)
    private Boolean balancePaid;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "remark", length = 500)
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getStyleId() {
        return styleId;
    }

    public void setStyleId(Long styleId) {
        this.styleId = styleId;
    }

    public Long getFabricId() {
        return fabricId;
    }

    public void setFabricId(Long fabricId) {
        this.fabricId = fabricId;
    }

    public Long getMeasurementId() {
        return measurementId;
    }

    public void setMeasurementId(Long measurementId) {
        this.measurementId = measurementId;
    }

    public BigDecimal getUsageMeters() {
        return usageMeters;
    }

    public void setUsageMeters(BigDecimal usageMeters) {
        this.usageMeters = usageMeters;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }

    public LocalDate getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(LocalDate deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    public BigDecimal getFabricCost() {
        return fabricCost;
    }

    public void setFabricCost(BigDecimal fabricCost) {
        this.fabricCost = fabricCost;
    }

    public BigDecimal getCraftFee() {
        return craftFee;
    }

    public void setCraftFee(BigDecimal craftFee) {
        this.craftFee = craftFee;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public BigDecimal getDeposit() {
        return deposit;
    }

    public void setDeposit(BigDecimal deposit) {
        this.deposit = deposit;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public Boolean getDepositPaid() {
        return depositPaid;
    }

    public void setDepositPaid(Boolean depositPaid) {
        this.depositPaid = depositPaid;
    }

    public Boolean getBalancePaid() {
        return balancePaid;
    }

    public void setBalancePaid(Boolean balancePaid) {
        this.balancePaid = balancePaid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
