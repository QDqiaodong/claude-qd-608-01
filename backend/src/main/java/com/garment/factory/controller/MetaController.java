package com.garment.factory.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.garment.factory.common.ApiResponse;
import com.garment.factory.domain.GarmentDict;
import com.garment.factory.service.MeasurementService;

/**
 * 一次性返回前端要用的全部字典：品类 / 成分 / 工序 / 状态 / 量体区间。
 */
@RestController
@RequestMapping("/api/meta")
public class MetaController {

    private final MeasurementService measurementService;

    public MetaController(MeasurementService measurementService) {
        this.measurementService = measurementService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> meta() {
        Map<String, Object> meta = new java.util.LinkedHashMap<>();
        meta.put("categories", GarmentDict.categories());
        meta.put("compositions", GarmentDict.compositions());
        meta.put("steps", GarmentDict.steps());
        meta.put("woStatuses", GarmentDict.woStatuses());
        meta.put("orderStatuses", GarmentDict.orderStatuses());
        meta.put("orderStatusOrder", GarmentDict.orderStatusOrder());
        meta.put("partRange", measurementService.partRange());
        meta.put("lowStockLine", GarmentDict.LOW_STOCK_LINE);
        meta.put("minLeadDays", GarmentDict.MIN_LEAD_DAYS);
        meta.put("maxUsage", GarmentDict.MAX_USAGE);
        meta.put("issueStatuses", GarmentDict.issueStatuses());
        meta.put("issueStatusOrder", GarmentDict.issueStatusOrder());
        meta.put("severities", GarmentDict.severities());
        meta.put("bodyParts", GarmentDict.bodyParts());
        meta.put("reworkStatuses", GarmentDict.reworkStatuses());
        return ApiResponse.success(meta);
    }
}
