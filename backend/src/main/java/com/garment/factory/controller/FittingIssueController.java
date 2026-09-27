package com.garment.factory.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.garment.factory.common.ApiResponse;
import com.garment.factory.domain.FittingIssue;
import com.garment.factory.domain.GarmentDict;
import com.garment.factory.domain.ReworkRound;
import com.garment.factory.dto.FittingActionReq;
import com.garment.factory.dto.FittingIssueCreateReq;
import com.garment.factory.dto.FittingIssueListItemDto;
import com.garment.factory.dto.FittingReworkStartReq;
import com.garment.factory.dto.SliceView;
import com.garment.factory.service.FittingIssueService;

/**
 * 试衣整改台：问题单登记、返修轮次、复核收口、重开、放行与操作流水。
 */
@RestController
@RequestMapping("/api/fitting-issues")
public class FittingIssueController {

    private final FittingIssueService fittingIssueService;

    public FittingIssueController(FittingIssueService fittingIssueService) {
        this.fittingIssueService = fittingIssueService;
    }

    /** 问题单列表：可按订单 / 状态 / 严重程度 / 只看未关闭过滤 */
    @GetMapping
    public ApiResponse<SliceView<FittingIssueListItemDto>> list(
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false, defaultValue = "false") boolean openOnly,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(fittingIssueService.list(orderId, status, severity, openOnly, keyword,
                page, size));
    }

    /** 问题单详情：基本信息 + 全部返修轮次（含历史结论）+ 操作流水 */
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.success(fittingIssueService.detail(id));
    }

    /** 建立试衣问题单（仅试衣中订单） */
    @PostMapping
    public ApiResponse<FittingIssue> create(@RequestBody FittingIssueCreateReq req) {
        return ApiResponse.success("试衣问题单已登记", fittingIssueService.create(req));
    }

    /** 发起返修：生成独立返修轮次，原工序工单不动 */
    @PostMapping("/{id}/rework")
    public ApiResponse<ReworkRound> startRework(@PathVariable Long id, @RequestBody FittingReworkStartReq req) {
        return ApiResponse.success("返修工单已发起", fittingIssueService.startRework(id, req));
    }

    /** 返修轮次完成：登记实际工时，完成后才能提交复核 */
    @PostMapping("/rounds/{roundId}/finish")
    public ApiResponse<ReworkRound> finishRound(@PathVariable Long roundId, @RequestBody FittingActionReq req) {
        return ApiResponse.success("返修轮次已完成", fittingIssueService.finishRework(roundId, req));
    }

    /** 提交复核：返修完成后进入待复核；普通问题也可从待处理直送复核 */
    @PostMapping("/{id}/submit-review")
    public ApiResponse<FittingIssue> submitReview(@PathVariable Long id, @RequestBody FittingActionReq req) {
        return ApiResponse.success("已提交复核", fittingIssueService.submitReview(id, req));
    }

    /** 复核通过：问题关闭，返修轮次同步收口 */
    @PostMapping("/{id}/review-pass")
    public ApiResponse<FittingIssue> pass(@PathVariable Long id, @RequestBody FittingActionReq req) {
        return ApiResponse.success("复核通过，问题已关闭", fittingIssueService.passReview(id, req));
    }

    /** 复核不通过：写明原因并在同事务开启下一轮返修 */
    @PostMapping("/{id}/review-reject")
    public ApiResponse<ReworkRound> reject(@PathVariable Long id, @RequestBody FittingActionReq req) {
        return ApiResponse.success("复核不通过，已开启下一轮返修", fittingIssueService.rejectReview(id, req));
    }

    /** 重新打开：交付前复检发现同一部位缺陷，生成新一轮返修，历史轮次与结论保留 */
    @PostMapping("/{id}/reopen")
    public ApiResponse<ReworkRound> reopen(@PathVariable Long id, @RequestBody FittingActionReq req) {
        return ApiResponse.success("问题已重新打开并发起新一轮返修", fittingIssueService.reopen(id, req));
    }

    /** 普通问题放行：明确确认并写明原因后留下放行记录 */
    @PostMapping("/{id}/release")
    public ApiResponse<FittingIssue> release(@PathVariable Long id, @RequestBody FittingActionReq req) {
        return ApiResponse.success("普通问题已确认放行", fittingIssueService.release(id, req));
    }

    /** 订单维度整改汇总：未关闭数 / 阻断数 / 当前返修阶段 / 完工闸口 */
    @GetMapping("/order/{orderId}/summary")
    public ApiResponse<Map<String, Object>> summary(@PathVariable Long orderId) {
        return ApiResponse.success(fittingIssueService.orderSummary(orderId));
    }

    /** 返修轮次列表：工序工单页与看板抽屉共用 */
    @GetMapping("/rounds")
    public ApiResponse<List<Map<String, Object>>> rounds(
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Long issueId) {
        return ApiResponse.success(fittingIssueService.rounds(orderId, issueId));
    }

    /** 订单整改操作流水 */
    @GetMapping("/order/{orderId}/logs")
    public ApiResponse<List<Map<String, Object>>> orderLogs(@PathVariable Long orderId) {
        return ApiResponse.success(fittingIssueService.orderLogs(orderId));
    }

    /** 整改台字典 */
    @GetMapping("/meta")
    public ApiResponse<Map<String, Object>> meta() {
        Map<String, Object> meta = new java.util.LinkedHashMap<>();
        meta.put("issueStatuses", GarmentDict.issueStatuses());
        meta.put("issueStatusOrder", GarmentDict.issueStatusOrder());
        meta.put("severities", GarmentDict.severities());
        meta.put("bodyParts", GarmentDict.bodyParts());
        meta.put("steps", GarmentDict.steps());
        meta.put("reworkStatuses", GarmentDict.reworkStatuses());
        meta.put("actions", GarmentDict.issueActions());
        return ApiResponse.success(meta);
    }
}
