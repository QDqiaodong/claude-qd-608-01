package com.garment.factory.dto;

import java.math.BigDecimal;

/**
 * 面料新增/编辑入参。
 */
public class FabricSaveReq {

    private Long id;
    private String fabricNo;
    private String name;
    private String composition;
    private String color;
    private String colorHex;
    private BigDecimal stockMeters;
    private BigDecimal unitPrice;
    private String supplier;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFabricNo() {
        return fabricNo;
    }

    public void setFabricNo(String fabricNo) {
        this.fabricNo = fabricNo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComposition() {
        return composition;
    }

    public void setComposition(String composition) {
        this.composition = composition;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getColorHex() {
        return colorHex;
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
    }

    public BigDecimal getStockMeters() {
        return stockMeters;
    }

    public void setStockMeters(BigDecimal stockMeters) {
        this.stockMeters = stockMeters;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }
}
