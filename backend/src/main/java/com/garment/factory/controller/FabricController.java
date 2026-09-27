package com.garment.factory.controller;

import java.math.BigDecimal;
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
import com.garment.factory.domain.Fabric;
import com.garment.factory.domain.GarmentDict;
import com.garment.factory.dto.FabricCardDto;
import com.garment.factory.dto.FabricSaveReq;
import com.garment.factory.dto.SliceView;
import com.garment.factory.service.FabricService;

/**
 * 面料库存：卡片墙 + 低库存预警。
 */
@RestController
@RequestMapping("/api/fabrics")
public class FabricController {

    private final FabricService fabricService;

    public FabricController(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    @GetMapping
    public ApiResponse<SliceView<FabricCardDto>> list(
            @RequestParam(required = false) String composition,
            @RequestParam(required = false) Boolean lowOnly,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {
        return ApiResponse.success(fabricService.list(composition, lowOnly, keyword, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<Fabric> detail(@PathVariable Long id) {
        return ApiResponse.success(fabricService.detail(id));
    }

    @PostMapping
    public ApiResponse<Fabric> save(@RequestBody FabricSaveReq req) {
        return ApiResponse.success("已保存面料", fabricService.save(req));
    }

    @PostMapping("/{id}/restock")
    public ApiResponse<Fabric> restock(@PathVariable Long id, @RequestBody Map<String, BigDecimal> body) {
        return ApiResponse.success("已补货", fabricService.restock(id, body.get("delta")));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> remove(@PathVariable Long id) {
        fabricService.remove(id);
        return ApiResponse.success("已删除面料", null);
    }

    /** 成分选项 + 低库存预警线 + 低库存数量 */
    @GetMapping("/meta")
    public ApiResponse<Map<String, Object>> meta() {
        Map<String, Object> meta = new java.util.LinkedHashMap<>();
        meta.put("compositions", GarmentDict.compositions());
        meta.put("lowStockLine", GarmentDict.LOW_STOCK_LINE);
        meta.put("lowStockCount", fabricService.lowStockCount());
        return ApiResponse.success(meta);
    }
}
