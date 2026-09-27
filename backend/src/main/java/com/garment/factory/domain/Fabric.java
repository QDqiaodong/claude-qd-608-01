package com.garment.factory.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 面料库存：按米计价。
 */
@Entity
@Table(name = "fabric")
public class Fabric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fabric_no", nullable = false, length = 30)
    private String fabricNo;

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Column(name = "composition", nullable = false, length = 20)
    private String composition;

    @Column(name = "color", nullable = false, length = 30)
    private String color;

    @Column(name = "color_hex", nullable = false, length = 10)
    private String colorHex;

    @Column(name = "stock_meters", nullable = false)
    private BigDecimal stockMeters;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "supplier", nullable = false, length = 80)
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
