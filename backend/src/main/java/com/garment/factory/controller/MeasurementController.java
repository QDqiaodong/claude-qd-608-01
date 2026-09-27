package com.garment.factory.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.garment.factory.common.ApiResponse;
import com.garment.factory.domain.Measurement;
import com.garment.factory.dto.MeasurementListItemDto;
import com.garment.factory.dto.MeasurementSaveReq;
import com.garment.factory.dto.SliceView;
import com.garment.factory.service.MeasurementService;

/**
 * 量体记录：客户 + 量体师 + 量体日期 + 各部位尺寸。
 */
@RestController
@RequestMapping("/api/measurements")
public class MeasurementController {

    private final MeasurementService measurementService;

    public MeasurementController(MeasurementService measurementService) {
        this.measurementService = measurementService;
    }

    @GetMapping
    public ApiResponse<SliceView<MeasurementListItemDto>> list(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String tailorName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {
        return ApiResponse.success(measurementService.list(customerId, tailorName, page, size));
    }

    /** 某客户的全部历史量体数据（下单向导第 2 步可复用） */
    @GetMapping("/by-customer/{customerId}")
    public ApiResponse<List<MeasurementListItemDto>> byCustomer(@PathVariable Long customerId) {
        return ApiResponse.success(measurementService.listByCustomer(customerId));
    }

    @GetMapping("/{id}")
    public ApiResponse<Measurement> detail(@PathVariable Long id) {
        return ApiResponse.success(measurementService.detail(id));
    }

    @GetMapping("/tailors")
    public ApiResponse<List<String>> tailors() {
        return ApiResponse.success(measurementService.tailors());
    }

    @PostMapping
    public ApiResponse<Measurement> save(@RequestBody MeasurementSaveReq req) {
        return ApiResponse.success("已保存量体记录", measurementService.save(req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> remove(@PathVariable Long id) {
        measurementService.remove(id);
        return ApiResponse.success("已删除量体记录", null);
    }
}
