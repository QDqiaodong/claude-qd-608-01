package com.garment.factory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 算价入参：款式 + 面料 + 用量 + 交期。
 */
public class PricePreviewReq {

    private Long styleId;
    private Long fabricId;
    private BigDecimal usageMeters;
    private LocalDate deliveryDate;

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
}
