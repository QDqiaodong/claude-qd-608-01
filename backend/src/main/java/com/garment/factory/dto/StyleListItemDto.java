package com.garment.factory.dto;

import java.math.BigDecimal;

/**
 * 款式列表行：由 JPQL 构造器表达式直接投影（SELECT new ...StyleListItemDto(...)）。
 */
public class StyleListItemDto {

    private final Long id;
    private final String styleNo;
    private final String name;
    private final String category;
    private final BigDecimal basePrice;
    private final Integer craftLevel;
    private final Boolean onSale;
    private final String remark;

    public StyleListItemDto(Long id, String styleNo, String name, String category,
                            BigDecimal basePrice, Integer craftLevel, Boolean onSale, String remark) {
        this.id = id;
        this.styleNo = styleNo;
        this.name = name;
        this.category = category;
        this.basePrice = basePrice;
        this.craftLevel = craftLevel;
        this.onSale = onSale;
        this.remark = remark;
    }

    public Long getId() {
        return id;
    }

    public String getStyleNo() {
        return styleNo;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public Integer getCraftLevel() {
        return craftLevel;
    }

    public Boolean getOnSale() {
        return onSale;
    }

    public String getRemark() {
        return remark;
    }
}
