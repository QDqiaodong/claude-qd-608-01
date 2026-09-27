package com.garment.factory.dto;

/**
 * 客户下拉选项（向导第 1 步：选客户）。
 */
public class CustomerOptionDto {

    private final Long id;
    private final String customerNo;
    private final String name;
    private final String phone;
    private final String vipLevel;
    private final Long orderCount;
    private final Long measureCount;

    public CustomerOptionDto(Long id, String customerNo, String name, String phone, String vipLevel,
                             Long orderCount, Long measureCount) {
        this.id = id;
        this.customerNo = customerNo;
        this.name = name;
        this.phone = phone;
        this.vipLevel = vipLevel;
        this.orderCount = orderCount == null ? 0L : orderCount;
        this.measureCount = measureCount == null ? 0L : measureCount;
    }

    public Long getId() {
        return id;
    }

    public String getCustomerNo() {
        return customerNo;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getVipLevel() {
        return vipLevel;
    }

    public Long getOrderCount() {
        return orderCount;
    }

    public Long getMeasureCount() {
        return measureCount;
    }
}
