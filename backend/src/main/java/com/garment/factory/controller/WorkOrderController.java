package com.garment.factory.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.garment.factory.common.ApiResponse;
import com.garment.factory.domain.GarmentDict;
import com.garment.factory.domain.WorkOrder;
import com.garment.factory.dto.SliceView;
import com.garment.factory.dto.WorkOrderAdvanceReq;
import com.garment.factory.dto.WorkOrderListItemDto;
import com.garment.factory.dto.WorkOrderSaveReq;
import com.garment.factory.service.WorkOrderService;

/**
 * 工序工单：表格 + 状态推进按钮 + 工时统计。
 */
@RestController
@RequestMapping("/api/work-orders")
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    public WorkOrderController(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    @GetMapping
    public ApiResponse<SliceView<WorkOrderListItemDto>> list(
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String step,
            @RequestParam(required = false) String owner,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(workOrderService.list(orderId, status, step, owner, keyword, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<WorkOrder> detail(@PathVariable Long id) {
        return ApiResponse.success(workOrderService.detail(id));
    }

    @PostMapping
    public ApiResponse<WorkOrder> save(@RequestBody WorkOrderSaveReq req) {
        return ApiResponse.success("已保存工序工单", workOrderService.save(req));
    }

    /** 状态推进：开工 / 完成 / 返工 */
    @PostMapping("/{id}/advance")
    public ApiResponse<WorkOrder> advance(@PathVariable Long id, @RequestBody WorkOrderAdvanceReq req) {
        return ApiResponse.success("工序工单已推进", workOrderService.advance(id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> remove(@PathVariable Long id) {
        workOrderService.remove(id);
        return ApiResponse.success("已删除工序工单", null);
    }

    /** 工时统计：按状态汇总 + 各工序计划/实际工时对比 */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats(@RequestParam(required = false) Long orderId) {
        return ApiResponse.success(workOrderService.stats(orderId));
    }

    /** 工序与工单状态选项 */
    @GetMapping("/meta")
    public ApiResponse<Map<String, Object>> meta() {
        Map<String, Object> meta = new java.util.LinkedHashMap<>();
        meta.put("steps", GarmentDict.steps());
        meta.put("statuses", GarmentDict.woStatuses());
        return ApiResponse.success(meta);
    }
}
