package com.garment.factory.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 试衣整改操作流水：建单 / 发起返修 / 返修完成 / 提交复核 / 复核通过/不通过 /
 * 重新打开 / 普通问题放行 / 订单完工 都记录操作人、时间、前后状态与原因，只追加不修改。
 */
@Entity
@Table(name = "fitting_action_log")
public class FittingActionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "issue_id")
    private Long issueId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "round_id")
    private Long roundId;

    /** CREATE/START_REWORK/FINISH_REWORK/SUBMIT_REVIEW/REVIEW_PASS/REVIEW_REJECT/REOPEN/RELEASE/FINISH_ORDER */
    @Column(name = "action", nullable = false, length = 30)
    private String action;

    @Column(name = "operator", nullable = false, length = 50)
    private String operator;

    @Column(name = "from_status", length = 20)
    private String fromStatus;

    @Column(name = "to_status", length = 20)
    private String toStatus;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "acted_at", nullable = false)
    private LocalDateTime actedAt;

    public FittingActionLog() {
    }

    public FittingActionLog(Long issueId, Long orderId, Long roundId, String action, String operator,
                            String fromStatus, String toStatus, String reason, LocalDateTime actedAt) {
        this.issueId = issueId;
        this.orderId = orderId;
        this.roundId = roundId;
        this.action = action;
        this.operator = operator;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason;
        this.actedAt = actedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getRoundId() {
        return roundId;
    }

    public void setRoundId(Long roundId) {
        this.roundId = roundId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(String fromStatus) {
        this.fromStatus = fromStatus;
    }

    public String getToStatus() {
        return toStatus;
    }

    public void setToStatus(String toStatus) {
        this.toStatus = toStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getActedAt() {
        return actedAt;
    }

    public void setActedAt(LocalDateTime actedAt) {
        this.actedAt = actedAt;
    }
}
