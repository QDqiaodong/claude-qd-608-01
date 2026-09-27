package com.garment.factory.dto;

import java.time.LocalDateTime;

/**
 * 试衣问题单列表项（JPQL 构造器表达式投影）。
 */
public class FittingIssueListItemDto {

    private final Long id;
    private final String issueNo;
    private final Long orderId;
    private final String orderNo;
    private final String customerName;
    private final String bodyPart;
    private final String severity;
    private final String description;
    private final String responsibleStep;
    private final String raisedBy;
    private final LocalDateTime raisedAt;
    private final String status;
    private final Boolean released;
    private final LocalDateTime closedAt;
    private final Integer reopenCount;
    private final Integer version;

    public FittingIssueListItemDto(Long id, String issueNo, Long orderId, String orderNo, String customerName,
                                   String bodyPart, String severity, String description, String responsibleStep,
                                   String raisedBy, LocalDateTime raisedAt, String status, Boolean released,
                                   LocalDateTime closedAt, Integer reopenCount, Integer version) {
        this.id = id;
        this.issueNo = issueNo;
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.customerName = customerName;
        this.bodyPart = bodyPart;
        this.severity = severity;
        this.description = description;
        this.responsibleStep = responsibleStep;
        this.raisedBy = raisedBy;
        this.raisedAt = raisedAt;
        this.status = status;
        this.released = released;
        this.closedAt = closedAt;
        this.reopenCount = reopenCount;
        this.version = version;
    }

    public Long getId() {
        return id;
    }

    public String getIssueNo() {
        return issueNo;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getBodyPart() {
        return bodyPart;
    }

    public String getSeverity() {
        return severity;
    }

    public String getDescription() {
        return description;
    }

    public String getResponsibleStep() {
        return responsibleStep;
    }

    public String getRaisedBy() {
        return raisedBy;
    }

    public LocalDateTime getRaisedAt() {
        return raisedAt;
    }

    public String getStatus() {
        return status;
    }

    public Boolean getReleased() {
        return released;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public Integer getReopenCount() {
        return reopenCount;
    }

    public Integer getVersion() {
        return version;
    }
}
