package com.garment.factory.controller;

import java.util.List;
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
import com.garment.factory.domain.CustomOrder;
import com.garment.factory.domain.GarmentDict;
import com.garment.factory.dto.CustomerOptionDto;
import com.garment.factory.dto.OrderAdvanceReq;
import com.garment.factory.dto.OrderCreateReq;
import com.garment.factory.dto.OrderListItemDto;
import com.garment.factory.dto.PricePreviewDto;
import com.garment.factory.dto.PricePreviewReq;
import com.garment.factory.dto.SliceView;
import com.garment.factory.service.OrderService;

/**
 * 定制订单：向导式下单、订单列表、进度看板、状态流转、尾款结算。
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** 列表：JPQL 构造器表达式投影 + Slice 滚动分页 */
    @GetMapping
    public ApiResponse<SliceView<OrderListItemDto>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {
        return ApiResponse.success(orderService.list(status, customerId, keyword, page, size));
    }

    /** 订单进度看板：按状态分列 */
    @GetMapping("/board")
    public ApiResponse<Map<String, Object>> board() {
        return ApiResponse.success(orderService.board());
    }

    /** 下单向导第 1 步：客户候选 */
    @GetMapping("/customers")
    public ApiResponse<List<CustomerOptionDto>> customers() {
        return ApiResponse.success(orderService.customers());
    }

    @GetMapping("/{id}")
    public ApiResponse<CustomOrder> detail(@PathVariable Long id) {
        return ApiResponse.success(orderService.detail(id));
    }

    /** 下单向导第 4 步：实时算价 */
    @PostMapping("/price-preview")
    public ApiResponse<PricePreviewDto> preview(@RequestBody PricePreviewReq req) {
        return ApiResponse.success(orderService.preview(req));
    }

    /** 下单向导：提交订单 */
    @PostMapping
    public ApiResponse<CustomOrder> create(@RequestBody OrderCreateReq req) {
        return ApiResponse.success("定制订单已建立，工序工单已投产", orderService.create(req));
    }

    /** 状态流转：默认推进到下一状态；试衣完工时普通问题放行需显式确认 */
    @PostMapping("/{id}/advance")
    public ApiResponse<CustomOrder> advance(@PathVariable Long id,
                                            @RequestBody(required = false) OrderAdvanceReq req) {
        return ApiResponse.success("订单状态已推进", orderService.advance(id, req));
    }

    /** 结清尾款 */
    @PostMapping("/{id}/balance-pay")
    public ApiResponse<CustomOrder> payBalance(@PathVariable Long id) {
        return ApiResponse.success("尾款已结清", orderService.payBalance(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> remove(@PathVariable Long id) {
        orderService.remove(id);
        return ApiResponse.success("订单已撤销，面料库存已归还", null);
    }

    /** 订单状态选项与流转顺序 */
    @GetMapping("/meta")
    public ApiResponse<Map<String, Object>> meta() {
        Map<String, Object> meta = new java.util.LinkedHashMap<>();
        meta.put("statuses", GarmentDict.orderStatuses());
        meta.put("statusOrder", GarmentDict.orderStatusOrder());
        return ApiResponse.success(meta);
    }
}
