package com.garment.factory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 向导第四步实时算价：用量 × 单价 + 款式基础价 = 总价。
 */
public class PricePreviewDto {

    private final BigDecimal usageMeters;
    private final BigDecimal unitPrice;
    private final BigDecimal fabricCost;
    private final BigDecimal basePrice;
    private final Integer craftLevel;
    private final BigDecimal totalPrice;
    private final BigDecimal deposit;
    private final BigDecimal balance;
    private final LocalDate deliveryDate;

    public PricePreviewDto(BigDecimal usageMeters, BigDecimal unitPrice, BigDecimal fabricCost, BigDecimal basePrice,
                           Integer craftLevel, BigDecimal totalPrice, BigDecimal deposit, BigDecimal balance,
                           LocalDate deliveryDate) {
        this.usageMeters = usageMeters;
        this.unitPrice = unitPrice;
        this.fabricCost = fabricCost;
        this.basePrice = basePrice;
        this.craftLevel = craftLevel;
        this.totalPrice = totalPrice;
        this.deposit = deposit;
        this.balance = balance;
        this.deliveryDate = deliveryDate;
    }

    public BigDecimal getUsageMeters() {
        return usageMeters;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getFabricCost() {
        return fabricCost;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public Integer getCraftLevel() {
        return craftLevel;
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

    public LocalDate getDeliveryDate() {
        return deliveryDate;
    }
}
