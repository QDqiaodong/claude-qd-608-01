package com.garment.factory.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.garment.factory.common.BizException;
import com.garment.factory.domain.CustomOrder;
import com.garment.factory.domain.FittingActionLog;
import com.garment.factory.domain.FittingIssue;
import com.garment.factory.domain.GarmentDict;
import com.garment.factory.domain.ReworkRound;
import com.garment.factory.dto.FittingActionReq;
import com.garment.factory.dto.FittingIssueCreateReq;
import com.garment.factory.dto.FittingIssueListItemDto;
import com.garment.factory.dto.FittingReworkStartReq;
import com.garment.factory.dto.SliceView;
import com.garment.factory.repository.CustomOrderRepository;
import com.garment.factory.repository.FittingActionLogRepository;
import com.garment.factory.repository.FittingIssueRepository;
import com.garment.factory.repository.ReworkRoundRepository;

/**
 * 试衣整改台：试衣问题单 → 返修轮次 → 复核收口的完整闭环。
 *
 * 并发约定：所有写流程都在同一事务内【先锁订单行，再锁问题单行】，加锁顺序全局一致，
 * 既串行化同一订单上的全部整改操作，也杜绝交叉加锁死锁；同一问题至多一张有效返修工单
 * 另由 fitting_rework_round 的生成列唯一索引兜底。
 */
@Service
public class FittingIssueService {

    private final FittingIssueRepository issueRepository;
    private final ReworkRoundRepository roundRepository;
    private final FittingActionLogRepository logRepository;
    private final CustomOrderRepository orderRepository;

    public FittingIssueService(FittingIssueRepository issueRepository, ReworkRoundRepository roundRepository,
                               FittingActionLogRepository logRepository, CustomOrderRepository orderRepository) {
        this.issueRepository = issueRepository;
        this.roundRepository = roundRepository;
        this.logRepository = logRepository;
        this.orderRepository = orderRepository;
    }

    // ---------------- 列表 / 看板汇总 ----------------

    @Transactional(readOnly = true)
    public SliceView<FittingIssueListItemDto> list(Long orderId, String status, String severity, boolean openOnly,
                                                   String keyword, int page, int size) {
        Slice<FittingIssueListItemDto> slice = issueRepository.sliceBy(orderId, trim(status), trim(severity),
                openOnly, trim(keyword), PageRequest.of(page, size));
        return SliceView.of(slice);
    }

    /**
     * 订单维度整改汇总：未关闭数量、阻断未关闭数、待返修工单数、当前处理阶段、放行记录等。
     * 整改台 / 订单看板 / 工序工单页看到的口径全部来自这里。
     */
    @Transactional(readOnly = true)
    public Map<String, Object> orderSummary(Long orderId) {
        CustomOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BizException("订单不存在"));
        return buildSummary(order);
    }

    @Transactional(readOnly = true)
    public Map<Long, Map<String, Object>> summariesByOrder(List<Long> orderIds) {
        Map<Long, Map<String, Object>> result = new LinkedHashMap<>();
        for (Long orderId : orderIds) {
            result.put(orderId, buildSummary(orderRepository.findById(orderId).orElse(null)));
        }
        return result;
    }

    private Map<String, Object> buildSummary(CustomOrder order) {
        Map<String, Object> summary = new LinkedHashMap<>();
        if (order == null) {
            return summary;
        }
        Long orderId = order.getId();
        List<FittingIssue> issues = issueRepository.findByOrderIdOrderByIdAsc(orderId);
        long openTotal = issues.stream().filter(i -> !"CLOSED".equals(i.getStatus())).count();
        long openBlocker = issues.stream()
                .filter(i -> "BLOCKER".equals(i.getSeverity()) && !"CLOSED".equals(i.getStatus()))
                .count();
        long released = issues.stream().filter(i -> Boolean.TRUE.equals(i.getReleased())).count();
        long activeRounds = roundRepository.countActiveByOrderId(orderId);
        long roundTotal = roundRepository.countByOrderId(orderId);

        // 当前处理阶段：取所有未关闭问题里流转最靠后的阶段，外加返修工单维度的提示
        int stageIndex = -1;
        String stage = issues.isEmpty() ? "NONE" : "ALL_CLOSED";
        for (FittingIssue issue : issues) {
            if ("CLOSED".equals(issue.getStatus())) {
                continue;
            }
            int idx = GarmentDict.issueStatusIndex(issue.getStatus());
            if (idx > stageIndex) {
                stageIndex = idx;
                stage = issue.getStatus();
            }
        }
        String blockingReason = null;
        if (openBlocker > 0) {
            blockingReason = "尚有 " + openBlocker + " 个阻断交付的试衣问题未关闭";
        } else if (activeRounds > 0) {
            blockingReason = "尚有 " + activeRounds + " 张返修工单未完成复核";
        }

        summary.put("orderId", orderId);
        summary.put("orderStatus", order.getStatus());
        summary.put("issueTotal", (long) issues.size());
        summary.put("openCount", openTotal);
        summary.put("openBlockerCount", openBlocker);
        summary.put("releasedCount", released);
        summary.put("activeRoundCount", activeRounds);
        summary.put("roundTotal", roundTotal);
        summary.put("currentStage", stage);
        summary.put("currentStageName", stageName(stage));
        summary.put("blockingReason", blockingReason);
        summary.put("finishBlocked", openBlocker > 0 || activeRounds > 0);
        summary.put("releaseRequired", openBlocker == 0 && activeRounds == 0
                && issues.stream().anyMatch(i -> !"CLOSED".equals(i.getStatus())
                        && !Boolean.TRUE.equals(i.getReleased())));
        return summary;
    }

    private String stageName(String stage) {
        return switch (stage) {
            case "NONE" -> "暂无试衣问题";
            case "ALL_CLOSED" -> "问题全部关闭";
            default -> GarmentDict.issueStatusName(stage);
        };
    }

    // ---------------- 问题单详情（含返修轮次 + 流水） ----------------

    @Transactional(readOnly = true)
    public Map<String, Object> detail(Long id) {
        FittingIssue issue = issueRepository.findById(id).orElseThrow(() -> new BizException("试衣问题单不存在"));
        return assembleDetail(issue);
    }

    private Map<String, Object> assembleDetail(FittingIssue issue) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("issue", issue);
        data.put("rounds", roundRepository.findByIssueIdOrderByRoundSeqAscIdAsc(issue.getId()));
        data.put("logs", toLogViews(logRepository.findByIssueIdOrderByActedAtAscIdAsc(issue.getId())));
        data.put("summary", buildSummary(orderRepository.findById(issue.getOrderId()).orElse(null)));
        return data;
    }

    private List<Map<String, Object>> toLogViews(List<FittingActionLog> logs) {
        List<Map<String, Object>> views = new ArrayList<>();
        for (FittingActionLog log : logs) {
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("id", log.getId());
            view.put("issueId", log.getIssueId());
            view.put("orderId", log.getOrderId());
            view.put("roundId", log.getRoundId());
            view.put("action", log.getAction());
            view.put("actionName", GarmentDict.issueActionName(log.getAction()));
            view.put("operator", log.getOperator());
            view.put("fromStatus", log.getFromStatus());
            view.put("fromStatusName", GarmentDict.issueStatusName(log.getFromStatus()));
            view.put("toStatus", log.getToStatus());
            view.put("toStatusName", GarmentDict.issueStatusName(log.getToStatus()));
            view.put("reason", log.getReason());
            view.put("actedAt", log.getActedAt());
            views.add(view);
        }
        return views;
    }

    // ---------------- 建单 ----------------

    @Transactional
    public FittingIssue create(FittingIssueCreateReq req) {
        if (req.getOrderId() == null) {
            throw new BizException("请选择所属订单");
        }
        // 先锁订单行：与后续返修/复核/订单推进共用同一把锁
        CustomOrder order = orderRepository.findByIdForUpdate(req.getOrderId())
                .orElseThrow(() -> new BizException("订单不存在"));
        if (!"FITTING".equals(order.getStatus())) {
            throw new BizException("只有处于「试衣中」的订单可以建立试衣问题单，当前订单状态为「"
                    + GarmentDict.orderStatusName(order.getStatus()) + "」");
        }
        if (!GarmentDict.isValidBodyPart(req.getBodyPart())) {
            throw new BizException("问题部位不合法");
        }
        if (!GarmentDict.isValidSeverity(req.getSeverity())) {
            throw new BizException("严重程度不合法");
        }
        if (req.getDescription() == null || req.getDescription().isBlank()) {
            throw new BizException("请填写问题描述");
        }
        if (req.getDescription().length() > 500) {
            throw new BizException("问题描述不能超过 500 字");
        }
        if (!GarmentDict.isValidStep(req.getResponsibleStep())) {
            throw new BizException("阻断交付的问题必须关联责任工序，请选择责任工序");
        }
        String raisedBy = requiredPerson(req.getRaisedBy(), "提出人");

        LocalDateTime now = LocalDateTime.now();
        FittingIssue issue = new FittingIssue();
        issue.setIssueNo(nextIssueNo());
        issue.setOrderId(order.getId());
        issue.setBodyPart(req.getBodyPart());
        issue.setSeverity(req.getSeverity());
        issue.setDescription(req.getDescription().trim());
        issue.setResponsibleStep(req.getResponsibleStep());
        issue.setRaisedBy(raisedBy);
        issue.setRaisedAt(now);
        issue.setStatus("PENDING");
        issue.setReleased(Boolean.FALSE);
        issue.setReopenCount(0);
        issue.setRemark(req.getRemark());
        FittingIssue saved = issueRepository.save(issue);
        logRepository.save(new FittingActionLog(saved.getId(), order.getId(), null, "CREATE", raisedBy,
                null, "PENDING", saved.getDescription(), now));
        return saved;
    }

    // ---------------- 发起返修（含复核不通过开下一轮、重开新轮） ----------------

    @Transactional
    public ReworkRound startRework(Long issueId, FittingReworkStartReq req) {
        CustomOrder[] locked = new CustomOrder[1];
        FittingIssue issue = lockOrderThenIssue(issueId, locked);
        requireFitting(locked[0], "发起返修");
        String operator = requiredPerson(req.getOperator(), "操作人");
        String owner = requiredPerson(req.getOwner(), "返修负责人");
        if (req.getPlannedHours() == null || req.getPlannedHours().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("请填写返修计划工时，且必须大于 0");
        }
        if (req.getPlannedHours().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BizException("单轮返修计划工时不能超过 100 小时");
        }
        String reason = requiredReason(req.getReworkReason(), "返修原因");
        checkVersion(issue, req.getExpectedVersion());

        // 阻断交付的问题必须走返修；普通问题同样允许发起返修
        String status = issue.getStatus();
        if (!"PENDING".equals(status) && !"REWORKING".equals(status)) {
            throw new BizException("当前状态为「" + GarmentDict.issueStatusName(status)
                    + "」，不能发起返修，请刷新后查看最新状态");
        }
        List<ReworkRound> active = roundRepository.findActiveByIssueId(issueId);
        if (!active.isEmpty()) {
            ReworkRound current = active.get(0);
            throw new BizException("该问题已存在一张有效返修工单「" + current.getRoundNo() + "」（第 "
                    + current.getRoundSeq() + " 轮，状态：" + GarmentDict.reworkStatusName(current.getStatus())
                    + "），不能重复发起，请刷新后查看");
        }
        // 阻断问题必须关联责任工序（建单时已强制，此处二次兜底）
        if ("BLOCKER".equals(issue.getSeverity())
                && !GarmentDict.isValidStep(issue.getResponsibleStep())) {
            throw new BizException("阻断交付的问题必须先关联责任工序，才能发起返修");
        }

        String before = issue.getStatus();
        ReworkRound round = openRound(issue, owner, req.getPlannedHours(), reason, operator, now());
        issue.setStatus("REWORKING");
        issueRepository.save(issue);
        // 处于返修中再发起（并发下的极端入口），同样以一条流水说明本轮来源
        logRepository.save(new FittingActionLog(issue.getId(), issue.getOrderId(), round.getId(),
                "START_REWORK", operator, before, "REWORKING", reason, now()));
        return round;
    }

    /**
     * 复核不通过：写明原因并立即开启下一轮返修。提交与开轮在同一事务、同一把锁内，
     * 因此并发的第二个不通过操作只会看到已变化的真实状态而被拒绝。
     */
    @Transactional
    public ReworkRound rejectReview(Long issueId, FittingActionReq req) {
        CustomOrder[] locked = new CustomOrder[1];
        FittingIssue issue = lockOrderThenIssue(issueId, locked);
        requireFitting(locked[0], "复核不通过");
        String reviewer = requiredPerson(req.getOperator(), "复核人");
        String reason = requiredReason(req.getReason(), "复核不通过原因");
        checkVersion(issue, req.getExpectedVersion());
        if (!"REVIEWING".equals(issue.getStatus())) {
            throw stale(issue, "登记复核不通过");
        }
        LocalDateTime ts = now();
        // 返修轮次复核不通过：当前轮次收口为 REJECTED；普通问题直送复核（无轮次）则直接开第 1 轮返修
        List<ReworkRound> active = roundRepository.findActiveByIssueId(issueId);
        ReworkRound current = null;
        if (!active.isEmpty()) {
            current = active.get(0);
            if (!"DONE".equals(current.getStatus())) {
                throw new BizException("返修工单「" + current.getRoundNo() + "」尚未完成，不能登记复核结果");
            }
            current.setStatus("REJECTED");
            current.setReviewedAt(ts);
            current.setReviewer(reviewer);
            current.setReviewComment(reason);
            roundRepository.save(current);
        }
        logRepository.save(new FittingActionLog(issue.getId(), issue.getOrderId(),
                current == null ? null : current.getId(),
                "REVIEW_REJECT", reviewer, "REVIEWING", "REWORKING", reason, ts));

        // 开启下一轮返修：责任人/计划工时默认沿用上一轮，可在请求里覆盖；原因必填不通过原因
        String owner = requiredPerson(req.getOwner() != null && !req.getOwner().isBlank()
                ? req.getOwner()
                : (current != null ? current.getOwner() : null), "下一轮返修负责人");
        BigDecimal planned = req.getPlannedHours() != null ? req.getPlannedHours()
                : (current != null ? current.getPlannedHours() : null);
        if (planned == null || planned.compareTo(BigDecimal.ZERO) <= 0
                || planned.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BizException("下一轮返修计划工时必须在 0~100 小时之间");
        }
        ReworkRound next = openRound(issue, owner, planned,
                clip("复核不通过，开启新一轮返修：" + reason), reviewer, ts);
        issue.setStatus("REWORKING");
        issueRepository.save(issue);
        logRepository.save(new FittingActionLog(issue.getId(), issue.getOrderId(), next.getId(),
                "START_REWORK", reviewer, "REWORKING", "REWORKING",
                "第 " + next.getRoundSeq() + " 轮返修：" + reason, ts));
        return next;
    }

    // ---------------- 返修完成 / 提交复核 / 复核通过 ----------------

    @Transactional
    public ReworkRound finishRework(Long roundId, FittingActionReq req) {
        ReworkRound round = roundRepository.findById(roundId)
                .orElseThrow(() -> new BizException("返修工单不存在"));
        CustomOrder[] locked = new CustomOrder[1];
        FittingIssue issue = lockOrderThenIssue(round.getIssueId(), locked);
        requireFitting(locked[0], "返修完成");
        String operator = requiredPerson(req.getOperator(), "操作人");
        if (!"RUNNING".equals(round.getStatus())) {
            throw new BizException("返修工单「" + round.getRoundNo() + "」当前为「"
                    + GarmentDict.reworkStatusName(round.getStatus()) + "」，不能登记返修完成，请刷新");
        }
        BigDecimal hours = req.getActualHours();
        if (hours == null || hours.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("返修完成必须填写实际工时，且必须大于 0");
        }
        BigDecimal limit = round.getPlannedHours().multiply(GarmentDict.MAX_HOURS_RATE);
        if (hours.compareTo(limit) > 0) {
            throw new BizException("返修实际工时不能超过计划工时的 " + GarmentDict.MAX_HOURS_RATE + " 倍（上限 "
                    + limit + " 小时）");
        }
        LocalDateTime ts = now();
        round.setStatus("DONE");
        round.setActualHours(hours);
        round.setFinishedAt(ts);
        if (req.getReason() != null && !req.getReason().isBlank()) {
            round.setReviewComment(req.getReason().trim());
        }
        roundRepository.save(round);
        // 返修完成只改变轮次，问题单仍处于返修中；提交复核后才进入待复核
        logRepository.save(new FittingActionLog(issue.getId(), issue.getOrderId(), round.getId(),
                "FINISH_REWORK", operator, issue.getStatus(), issue.getStatus(),
                "第 " + round.getRoundSeq() + " 轮返修完成，实耗 " + hours + " 小时"
                        + (req.getReason() != null && !req.getReason().isBlank()
                        ? "：" + req.getReason().trim() : ""), ts));
        return round;
    }

    @Transactional
    public FittingIssue submitReview(Long issueId, FittingActionReq req) {
        CustomOrder[] locked = new CustomOrder[1];
        FittingIssue issue = lockOrderThenIssue(issueId, locked);
        requireFitting(locked[0], "提交复核");
        String operator = requiredPerson(req.getOperator(), "操作人");
        checkVersion(issue, req.getExpectedVersion());

        LocalDateTime ts = now();
        if ("PENDING".equals(issue.getStatus())) {
            // 普通问题可以直接进入待复核（无需返修）
            if ("BLOCKER".equals(issue.getSeverity())) {
                throw new BizException("阻断交付的问题必须先发起返修，返修完成后才能提交复核");
            }
            issue.setStatus("REVIEWING");
            issueRepository.save(issue);
            logRepository.save(new FittingActionLog(issue.getId(), issue.getOrderId(), null,
                    "SUBMIT_REVIEW", operator, "PENDING", "REVIEWING",
                    requiredReason(req.getReason(), "提交复核说明"), ts));
            return issue;
        }
        if (!"REWORKING".equals(issue.getStatus())) {
            throw stale(issue, "提交复核");
        }
        ReworkRound current = requireCurrentRound(issueId);
        if (!"DONE".equals(current.getStatus())) {
            throw new BizException("返修工单「" + current.getRoundNo() + "」尚未完成（"
                    + GarmentDict.reworkStatusName(current.getStatus()) + "），返修轮次完成后才能提交复核");
        }
        issue.setStatus("REVIEWING");
        issueRepository.save(issue);
        logRepository.save(new FittingActionLog(issue.getId(), issue.getOrderId(), current.getId(),
                "SUBMIT_REVIEW", operator, "REWORKING", "REVIEWING",
                "第 " + current.getRoundSeq() + " 轮返修完成，提交复核", ts));
        return issue;
    }

    @Transactional
    public FittingIssue passReview(Long issueId, FittingActionReq req) {
        CustomOrder[] locked = new CustomOrder[1];
        FittingIssue issue = lockOrderThenIssue(issueId, locked);
        requireFitting(locked[0], "复核通过");
        String reviewer = requiredPerson(req.getOperator(), "复核人");
        checkVersion(issue, req.getExpectedVersion());
        if (!"REVIEWING".equals(issue.getStatus())) {
            throw stale(issue, "复核通过");
        }
        LocalDateTime ts = now();
        String comment = req.getReason() == null || req.getReason().isBlank() ? "复核通过" : req.getReason().trim();
        // 若有返修轮次，轮次同步收口为 PASSED；普通问题直送复核时无轮次
        List<ReworkRound> active = roundRepository.findActiveByIssueId(issueId);
        for (ReworkRound round : active) {
            if ("DONE".equals(round.getStatus())) {
                round.setStatus("PASSED");
                round.setReviewedAt(ts);
                round.setReviewer(reviewer);
                round.setReviewComment(comment);
                roundRepository.save(round);
            }
        }
        String before = issue.getStatus();
        issue.setStatus("CLOSED");
        issue.setClosedAt(ts);
        issueRepository.save(issue);
        logRepository.save(new FittingActionLog(issue.getId(), issue.getOrderId(),
                active.isEmpty() ? null : active.get(0).getId(),
                "REVIEW_PASS", reviewer, before, "CLOSED", comment, ts));
        return issue;
    }

    // ---------------- 重新打开（交付前再现同一部位缺陷） ----------------

    @Transactional
    public ReworkRound reopen(Long issueId, FittingActionReq req) {
        CustomOrder[] locked = new CustomOrder[1];
        FittingIssue issue = lockOrderThenIssue(issueId, locked);
        CustomOrder order = locked[0];
        if ("DELIVERED".equals(order.getStatus())) {
            throw new BizException("订单已交付，试衣问题单不能再重新打开");
        }
        if (!"CLOSED".equals(issue.getStatus())) {
            throw stale(issue, "重新打开");
        }
        String operator = requiredPerson(req.getOperator(), "操作人");
        String reason = requiredReason(req.getReason(), "重新打开原因");
        String owner = requiredPerson(req.getOwner(), "新一轮返修负责人");
        BigDecimal planned = req.getPlannedHours();
        if (planned == null || planned.compareTo(BigDecimal.ZERO) <= 0
                || planned.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BizException("请填写新一轮返修的计划工时（0~100 小时）");
        }
        String step = req.getResponsibleStep() != null && !req.getResponsibleStep().isBlank()
                ? req.getResponsibleStep() : issue.getResponsibleStep();
        if (!GarmentDict.isValidStep(step)) {
            throw new BizException("责任工序不合法，无法重新打开");
        }
        checkVersion(issue, req.getExpectedVersion());

        LocalDateTime ts = now();
        String before = issue.getStatus();
        issue.setStatus("REWORKING");
        issue.setClosedAt(null);
        issue.setReopenCount(issue.getReopenCount() == null ? 1 : issue.getReopenCount() + 1);
        issue.setReleased(Boolean.FALSE);
        issue.setResponsibleStep(step);
        issueRepository.save(issue);
        ReworkRound round = openRound(issue, owner, planned,
                clip("交付前复检发现同一部位缺陷，重新打开：" + reason), operator, ts);
        logRepository.save(new FittingActionLog(issue.getId(), issue.getOrderId(), round.getId(),
                "REOPEN", operator, before, "REWORKING",
                "第 " + round.getRoundSeq() + " 次重开（" + GarmentDict.bodyPartName(issue.getBodyPart())
                        + "）：" + reason, ts));
        logRepository.save(new FittingActionLog(issue.getId(), issue.getOrderId(), round.getId(),
                "START_REWORK", operator, "REWORKING", "REWORKING",
                "重新打开后发起第 " + round.getRoundSeq() + " 轮返修：" + reason, ts));

        // 订单若已完工（未交付），退回试衣中，保证看板/工单页/整改台口径一致且不回退已关闭历史
        if ("FINISHED".equals(order.getStatus())) {
            orderRepository.updateStatus(order.getId(), "FITTING");
            order.setStatus("FITTING");
        }
        return round;
    }

    // ---------------- 普通问题放行 ----------------

    @Transactional
    public FittingIssue release(Long issueId, FittingActionReq req) {
        CustomOrder[] locked = new CustomOrder[1];
        FittingIssue issue = lockOrderThenIssue(issueId, locked);
        CustomOrder order = locked[0];
        if (!"FITTING".equals(order.getStatus())) {
            throw new BizException("只有试衣中的订单可以放行普通问题，当前为「"
                    + GarmentDict.orderStatusName(order.getStatus()) + "」");
        }
        if ("CLOSED".equals(issue.getStatus())) {
            throw new BizException("问题已关闭，无需放行");
        }
        if (Boolean.TRUE.equals(issue.getReleased())) {
            throw new BizException("该问题此前已确认放行，请勿重复操作");
        }
        if ("BLOCKER".equals(issue.getSeverity())) {
            throw new BizException("阻断交付的问题不能放行，必须返修并复核关闭");
        }
        String operator = requiredPerson(req.getOperator(), "放行人");
        String reason = requiredReason(req.getReason(), "放行确认说明");
        checkVersion(issue, req.getExpectedVersion());
        LocalDateTime ts = now();
        String before = issue.getStatus();
        issue.setReleased(Boolean.TRUE);
        issueRepository.save(issue);
        logRepository.save(new FittingActionLog(issue.getId(), issue.getOrderId(), null,
                "RELEASE", operator, before, before,
                "普通问题经确认随单放行：" + reason, ts));
        return issue;
    }

    // ---------------- 订单完工收口（供 OrderService 在同一事务内调用） ----------------

    /**
     * 完工前检查：阻断问题必须全部关闭、返修工单必须全部收口；普通问题未关闭则返回待确认清单。
     * 本方法只检查不放行；order 参数必须是已经持有行锁的实例。
     */
    @Transactional
    public List<FittingIssue> checkCanFinish(CustomOrder order) {
        List<FittingIssue> issues = issueRepository.findByOrderIdOrderByIdAsc(order.getId());
        long openBlocker = issues.stream()
                .filter(i -> "BLOCKER".equals(i.getSeverity()) && !"CLOSED".equals(i.getStatus()))
                .count();
        if (openBlocker > 0) {
            throw new BizException("尚有 " + openBlocker + " 个阻断交付的试衣问题未关闭，不能流转到「已完成」");
        }
        long activeRounds = roundRepository.countActiveByOrderId(order.getId());
        if (activeRounds > 0) {
            throw new BizException("尚有 " + activeRounds + " 张返修工单未完成复核，不能流转到「已完成」");
        }
        return issues.stream()
                .filter(i -> !"CLOSED".equals(i.getStatus()) && !Boolean.TRUE.equals(i.getReleased()))
                .toList();
    }

    /**
     * 带普通问题放行确认的完工收口：在订单事务持锁期间调用。
     * 对每张未关闭普通问题逐条写明放行原因并落流水，绝不悄悄略过。
     */
    @Transactional
    public void releaseAndLogOnFinish(CustomOrder order, String operator, String globalReason) {
        List<FittingIssue> pending = checkCanFinish(order);
        if (!pending.isEmpty()) {
            String who = requiredPerson(operator, "操作人");
            String reason = requiredReason(globalReason, "普通问题放行确认说明");
            LocalDateTime ts = now();
            for (FittingIssue issue : pending) {
                if ("BLOCKER".equals(issue.getSeverity())) {
                    // 理论上 checkCanFinish 已拦，双保险
                    throw new BizException("阻断交付的问题不能随单放行");
                }
                String before = issue.getStatus();
                issue.setReleased(Boolean.TRUE);
                issueRepository.save(issue);
                logRepository.save(new FittingActionLog(issue.getId(), order.getId(), null,
                        "RELEASE", who, before, before, "完工推进时确认随单放行：" + reason, ts));
            }
        }
        LocalDateTime ts = now();
        logRepository.save(new FittingActionLog(null, order.getId(), null, "FINISH_ORDER",
                operator == null || operator.isBlank() ? "系统" : operator.trim(),
                order.getStatus(), "FINISHED",
                pending.isEmpty() ? "试衣问题均已关闭，订单完工"
                        : "含 " + pending.size() + " 个已确认放行的普通问题，订单完工", ts));
    }

    // ---------------- 返修轮次 / 订单流水查询（工单页、看板复用） ----------------

    @Transactional(readOnly = true)
    public List<Map<String, Object>> rounds(Long orderId, Long issueId) {
        List<ReworkRound> rounds = issueId != null
                ? roundRepository.findByIssueIdOrderByRoundSeqAscIdAsc(issueId)
                : (orderId != null ? roundRepository.findByOrderIdOrderByIdAsc(orderId)
                        : roundRepository.findAll());
        List<Map<String, Object>> views = new ArrayList<>();
        for (ReworkRound round : rounds) {
            views.add(roundView(round));
        }
        return views;
    }

    private Map<String, Object> roundView(ReworkRound round) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", round.getId());
        view.put("roundNo", round.getRoundNo());
        view.put("issueId", round.getIssueId());
        view.put("orderId", round.getOrderId());
        view.put("roundSeq", round.getRoundSeq());
        view.put("responsibleStep", round.getResponsibleStep());
        view.put("responsibleStepName", GarmentDict.stepName(round.getResponsibleStep()));
        view.put("owner", round.getOwner());
        view.put("plannedHours", round.getPlannedHours());
        view.put("actualHours", round.getActualHours());
        view.put("reworkReason", round.getReworkReason());
        view.put("status", round.getStatus());
        view.put("statusName", GarmentDict.reworkStatusName(round.getStatus()));
        view.put("startedAt", round.getStartedAt());
        view.put("finishedAt", round.getFinishedAt());
        view.put("reviewedAt", round.getReviewedAt());
        view.put("reviewer", round.getReviewer());
        view.put("reviewComment", round.getReviewComment());
        FittingIssue issue = issueRepository.findById(round.getIssueId()).orElse(null);
        if (issue != null) {
            view.put("issueNo", issue.getIssueNo());
            view.put("bodyPart", issue.getBodyPart());
            view.put("bodyPartName", GarmentDict.bodyPartName(issue.getBodyPart()));
            view.put("severity", issue.getSeverity());
            view.put("severityName", GarmentDict.severityName(issue.getSeverity()));
            view.put("issueStatus", issue.getStatus());
            view.put("issueStatusName", GarmentDict.issueStatusName(issue.getStatus()));
        }
        CustomOrder order = orderRepository.findById(round.getOrderId()).orElse(null);
        if (order != null) {
            view.put("orderNo", order.getOrderNo());
        }
        return view;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> orderLogs(Long orderId) {
        if (!orderRepository.existsById(orderId)) {
            throw new BizException("订单不存在");
        }
        return toLogViews(logRepository.findByOrderIdOrderByActedAtAscIdAsc(orderId));
    }

    // ---------------- 内部辅助 ----------------

    /**
     * 统一加锁入口：先订单行锁、后问题行锁。
     * 两个并发操作（如同问题同时发起返修）会在订单锁上串行，第二个事务拿到锁后读到的已是新状态。
     */
    private FittingIssue lockOrderThenIssue(Long issueId, CustomOrder[] orderHolder) {
        FittingIssue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new BizException("试衣问题单不存在"));
        CustomOrder order = orderRepository.findByIdForUpdate(issue.getOrderId())
                .orElseThrow(() -> new BizException("订单不存在"));
        orderHolder[0] = order;
        try {
            return issueRepository.findByIdForUpdate(issueId)
                    .orElseThrow(() -> new BizException("试衣问题单不存在"));
        } catch (ObjectOptimisticLockingFailureException e) {
            throw conflict(issue);
        }
    }

    private ReworkRound openRound(FittingIssue issue, String owner, BigDecimal planned, String reason,
                                  String operator, LocalDateTime ts) {
        int seq = roundRepository.findByIssueIdOrderByRoundSeqAscIdAsc(issue.getId()).size() + 1;
        ReworkRound round = new ReworkRound();
        round.setRoundNo(nextRoundNo());
        round.setIssueId(issue.getId());
        round.setOrderId(issue.getOrderId());
        round.setRoundSeq(seq);
        round.setResponsibleStep(issue.getResponsibleStep());
        round.setOwner(owner);
        round.setPlannedHours(planned);
        round.setActualHours(BigDecimal.ZERO);
        round.setReworkReason(reason);
        round.setStatus("RUNNING");
        round.setStartedAt(ts);
        try {
            return roundRepository.save(round);
        } catch (DataIntegrityViolationException e) {
            // uk_frr_active 兜底：并发下第二个事务在此撞唯一索引
            throw new BizException("该问题已有一张进行中的返修工单，并发发起已被拦截，请刷新后查看最新返修进度");
        }
    }

    private ReworkRound requireCurrentRound(Long issueId) {
        List<ReworkRound> active = roundRepository.findActiveByIssueId(issueId);
        if (active.isEmpty()) {
            throw new BizException("该问题没有进行中的返修工单，请刷新后查看");
        }
        return active.get(0);
    }

    private void requireFitting(CustomOrder order, String action) {
        if ("DELIVERED".equals(order.getStatus())) {
            throw new BizException("订单已交付，不能再" + action);
        }
        if (!"FITTING".equals(order.getStatus())) {
            throw new BizException("订单当前为「" + GarmentDict.orderStatusName(order.getStatus())
                    + "」，请先退回/推进到试衣环节后再操作");
        }
    }

    private void checkVersion(FittingIssue issue, Integer expectedVersion) {
        if (expectedVersion != null && !expectedVersion.equals(issue.getVersion())) {
            throw new BizException("页面数据已过期（当前第 " + issue.getVersion() + " 版，状态「"
                    + GarmentDict.issueStatusName(issue.getStatus()) + "」），请刷新后重试");
        }
    }

    private BizException stale(FittingIssue issue, String action) {
        return new BizException("数据已被其他同事更新：问题「" + issue.getIssueNo() + "」当前为「"
                + GarmentDict.issueStatusName(issue.getStatus()) + "」，不能" + action
                + "，请刷新后按真实状态操作");
    }

    private BizException conflict(FittingIssue issue) {
        return new BizException("操作冲突，问题「" + issue.getIssueNo() + "」刚被其他同事更新，"
                + "请刷新后查看真实状态");
    }

    private String requiredPerson(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BizException("请填写" + field);
        }
        return value.trim();
    }

    private String requiredReason(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BizException(field == null || field.contains("说明") || field.contains("原因")
                    ? "请填写" + field : field + "不能为空");
        }
        String text = value.trim();
        if (text.length() > 500) {
            throw new BizException("原因/说明不能超过 500 字");
        }
        return text;
    }

    private LocalDateTime now() {
        return LocalDateTime.now();
    }

    private String clip(String text) {
        if (text == null) {
            return null;
        }
        return text.length() > 500 ? text.substring(0, 500) : text;
    }

    private String nextIssueNo() {
        long seq = issueRepository.count() + 1;
        String no;
        do {
            no = String.format("FI-%04d", 5000 + seq);
            seq++;
        } while (issueRepository.existsByIssueNo(no));
        return no;
    }

    private String nextRoundNo() {
        long seq = roundRepository.count() + 1;
        String no;
        do {
            no = String.format("RR-%04d", 6000 + seq);
            seq++;
        } while (roundRepository.existsByRoundNo(no));
        return no;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
