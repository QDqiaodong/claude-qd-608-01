package com.garment.factory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 向导第四步下单入参（第 2 步的量体可现场新建）。
 */
public class OrderCreateReq {

    private Long customerId;
    private Long styleId;
    private Long fabricId;
    private Long measurementId;
    private BigDecimal usageMeters;
    private LocalDate deliveryDate;
    private Boolean payDeposit;
    private String remark;

    /** 现场录量体时携带的尺寸信息 */
    private MeasurementSaveReq measurement;

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

    public LocalDate getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(LocalDate deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    public Boolean getPayDeposit() {
        return payDeposit;
    }

    public void setPayDeposit(Boolean payDeposit) {
        this.payDeposit = payDeposit;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public MeasurementSaveReq getMeasurement() {
        return measurement;
    }

    public void setMeasurement(MeasurementSaveReq measurement) {
        this.measurement = measurement;
    }
}
