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
 * 量体记录：客户各部位净尺寸。
 */
@Entity
@Table(name = "measurement")
public class Measurement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "record_no", nullable = false, length = 30)
    private String recordNo;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "customer_name", nullable = false, length = 50)
    private String customerName;

    @Column(name = "tailor_name", nullable = false, length = 50)
    private String tailorName;

    @Column(name = "measure_date", nullable = false)
    private LocalDate measureDate;

    @Column(name = "bust", nullable = false)
    private BigDecimal bust;

    @Column(name = "waist", nullable = false)
    private BigDecimal waist;

    @Column(name = "hip", nullable = false)
    private BigDecimal hip;

    @Column(name = "shoulder", nullable = false)
    private BigDecimal shoulder;

    @Column(name = "sleeve_len", nullable = false)
    private BigDecimal sleeveLength;

    @Column(name = "body_len", nullable = false)
    private BigDecimal bodyLength;

    @Column(name = "pants_len", nullable = false)
    private BigDecimal pantsLength;

    @Column(name = "remark", length = 500)
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRecordNo() {
        return recordNo;
    }

    public void setRecordNo(String recordNo) {
        this.recordNo = recordNo;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getTailorName() {
        return tailorName;
    }

    public void setTailorName(String tailorName) {
        this.tailorName = tailorName;
    }

    public LocalDate getMeasureDate() {
        return measureDate;
    }

    public void setMeasureDate(LocalDate measureDate) {
        this.measureDate = measureDate;
    }

    public BigDecimal getBust() {
        return bust;
    }

    public void setBust(BigDecimal bust) {
        this.bust = bust;
    }

    public BigDecimal getWaist() {
        return waist;
    }

    public void setWaist(BigDecimal waist) {
        this.waist = waist;
    }

    public BigDecimal getHip() {
        return hip;
    }

    public void setHip(BigDecimal hip) {
        this.hip = hip;
    }

    public BigDecimal getShoulder() {
        return shoulder;
    }

    public void setShoulder(BigDecimal shoulder) {
        this.shoulder = shoulder;
    }

    public BigDecimal getSleeveLength() {
        return sleeveLength;
    }

    public void setSleeveLength(BigDecimal sleeveLength) {
        this.sleeveLength = sleeveLength;
    }

    public BigDecimal getBodyLength() {
        return bodyLength;
    }

    public void setBodyLength(BigDecimal bodyLength) {
        this.bodyLength = bodyLength;
    }

    public BigDecimal getPantsLength() {
        return pantsLength;
    }

    public void setPantsLength(BigDecimal pantsLength) {
        this.pantsLength = pantsLength;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
