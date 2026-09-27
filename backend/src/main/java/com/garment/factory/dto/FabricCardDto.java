package com.garment.factory.dto;

import java.math.BigDecimal;

/**
 * 面料卡片：由 JPQL 构造器表达式直接投影（含低库存预警标记）。
 */
public class FabricCardDto {

    private final Long id;
    private final String fabricNo;
    private final String name;
    private final String composition;
    private final String color;
    private final String colorHex;
    private final BigDecimal stockMeters;
    private final BigDecimal unitPrice;
    private final String supplier;
    private final Boolean lowStock;

    public FabricCardDto(Long id, String fabricNo, String name, String composition,
                         String color, String colorHex, BigDecimal stockMeters, BigDecimal unitPrice,
                         String supplier, Boolean lowStock) {
        this.id = id;
        this.fabricNo = fabricNo;
        this.name = name;
        this.composition = composition;
        this.color = color;
        this.colorHex = colorHex;
        this.stockMeters = stockMeters;
        this.unitPrice = unitPrice;
        this.supplier = supplier;
        this.lowStock = lowStock;
    }

    public Long getId() {
        return id;
    }

    public String getFabricNo() {
        return fabricNo;
    }

    public String getName() {
        return name;
    }

    public String getComposition() {
        return composition;
    }

    public String getColor() {
        return color;
    }

    public String getColorHex() {
        return colorHex;
    }

    public BigDecimal getStockMeters() {
        return stockMeters;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public String getSupplier() {
        return supplier;
    }

    public Boolean getLowStock() {
        return lowStock;
    }
}
