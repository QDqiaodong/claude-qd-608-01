package com.garment.factory.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * 试衣问题单：订单进入试衣（FITTING）后登记的问题。
 * 状态机：PENDING(待处理) → REWORKING(返修中) → REVIEWING(待复核) → CLOSED(已关闭)。
 * BLOCKER（阻断交付）必须关联责任工序并发起返修；MINOR（普通）可直接进入待复核或完工时确认放行。
 */
@Entity
@Table(name = "fitting_issue")
public class FittingIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "issue_no", nullable = false, length = 30)
    private String issueNo;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "body_part", nullable = false, length = 30)
    private String bodyPart;

    /** MINOR 普通问题 / BLOCKER 阻断交付 */
    @Column(name = "severity", nullable = false, length = 20)
    private String severity;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "responsible_step", nullable = false, length = 20)
    private String responsibleStep;

    @Column(name = "raised_by", nullable = false, length = 50)
    private String raisedBy;

    @Column(name = "raised_at", nullable = false)
    private LocalDateTime raisedAt;

    /** PENDING / REWORKING / REVIEWING / CLOSED */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /** 普通问题在订单完工时是否已经明确确认放行 */
    @Column(name = "released", nullable = false)
    private Boolean released = Boolean.FALSE;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "reopen_count", nullable = false)
    private Integer reopenCount = 0;

    /** 乐观版本：前端携带版本提交时可识别过期页面，并发冲突以服务端行锁结果为准 */
    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    @Column(name = "remark", length = 500)
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIssueNo() {
        return issueNo;
    }

    public void setIssueNo(String issueNo) {
        this.issueNo = issueNo;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getBodyPart() {
        return bodyPart;
    }

    public void setBodyPart(String bodyPart) {
        this.bodyPart = bodyPart;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getResponsibleStep() {
        return responsibleStep;
    }

    public void setResponsibleStep(String responsibleStep) {
        this.responsibleStep = responsibleStep;
    }

    public String getRaisedBy() {
        return raisedBy;
    }

    public void setRaisedBy(String raisedBy) {
        this.raisedBy = raisedBy;
    }

    public LocalDateTime getRaisedAt() {
        return raisedAt;
    }

    public void setRaisedAt(LocalDateTime raisedAt) {
        this.raisedAt = raisedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getReleased() {
        return released;
    }

    public void setReleased(Boolean released) {
        this.released = released;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public Integer getReopenCount() {
        return reopenCount;
    }

    public void setReopenCount(Integer reopenCount) {
        this.reopenCount = reopenCount;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
