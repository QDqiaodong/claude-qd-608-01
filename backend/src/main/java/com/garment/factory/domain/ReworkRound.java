package com.garment.factory.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 试衣返修轮次：每次发起返修（含复核不通过开启下一轮、关闭后重新打开）都生成独立轮次。
 * 不覆盖原工序工单的已完成事实与历史工时；计划/实际工时、返修原因、复核结论都落在轮次上。
 * status：RUNNING(返修中) / DONE(已返修完成待复核) / PASSED(复核通过) / REJECTED(复核不通过)。
 * 数据库唯一索引 uk_frr_active 保证同一问题至多一张 RUNNING/DONE 的有效当前返修工单。
 */
@Entity
@Table(name = "fitting_rework_round")
public class ReworkRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "round_no", nullable = false, length = 30)
    private String roundNo;

    @Column(name = "issue_id", nullable = false)
    private Long issueId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "round_seq", nullable = false)
    private Integer roundSeq;

    @Column(name = "responsible_step", nullable = false, length = 20)
    private String responsibleStep;

    @Column(name = "owner", nullable = false, length = 50)
    private String owner;

    @Column(name = "planned_hours", nullable = false)
    private BigDecimal plannedHours;

    @Column(name = "actual_hours", nullable = false)
    private BigDecimal actualHours = BigDecimal.ZERO;

    @Column(name = "rework_reason", nullable = false, length = 500)
    private String reworkReason;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewer", length = 50)
    private String reviewer;

    @Column(name = "review_comment", length = 500)
    private String reviewComment;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(String roundNo) {
        this.roundNo = roundNo;
    }

    public Long getIssueId() {
        return issueId;
    }

    public void setIssueId(Long issueId) {
        this.issueId = issueId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Integer getRoundSeq() {
        return roundSeq;
    }

    public void setRoundSeq(Integer roundSeq) {
        this.roundSeq = roundSeq;
    }

    public String getResponsibleStep() {
        return responsibleStep;
    }

    public void setResponsibleStep(String responsibleStep) {
        this.responsibleStep = responsibleStep;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public BigDecimal getPlannedHours() {
        return plannedHours;
    }

    public void setPlannedHours(BigDecimal plannedHours) {
        this.plannedHours = plannedHours;
    }

    public BigDecimal getActualHours() {
        return actualHours;
    }

    public void setActualHours(BigDecimal actualHours) {
        this.actualHours = actualHours;
    }

    public String getReworkReason() {
        return reworkReason;
    }

    public void setReworkReason(String reworkReason) {
        this.reworkReason = reworkReason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getReviewer() {
        return reviewer;
    }

    public void setReviewer(String reviewer) {
        this.reviewer = reviewer;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }
}
