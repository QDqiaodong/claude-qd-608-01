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
import com.garment.factory.domain.Style;
import com.garment.factory.dto.SliceView;
import com.garment.factory.dto.StyleListItemDto;
import com.garment.factory.dto.StyleSaveReq;
import com.garment.factory.service.StyleService;

/**
 * 款式档案：款号 / 名称 / 品类 / 基础价 / 工艺难度 / 是否在售。
 */
@RestController
@RequestMapping("/api/styles")
public class StyleController {

    private final StyleService styleService;

    public StyleController(StyleService styleService) {
        this.styleService = styleService;
    }

    /** 列表：JPQL 构造器表达式投影 + Slice 滚动分页 */
    @GetMapping
    public ApiResponse<SliceView<StyleListItemDto>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean onSale,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {
        return ApiResponse.success(styleService.list(category, onSale, keyword, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<Style> detail(@PathVariable Long id) {
        return ApiResponse.success(styleService.detail(id));
    }

    @PostMapping
    public ApiResponse<Style> save(@RequestBody StyleSaveReq req) {
        return ApiResponse.success("已保存款式", styleService.save(req));
    }

    @PostMapping("/{id}/sale")
    public ApiResponse<Style> toggleSale(@PathVariable Long id) {
        return ApiResponse.success("已切换上下架状态", styleService.toggleSale(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> remove(@PathVariable Long id) {
        styleService.remove(id);
        return ApiResponse.success("已删除款式", null);
    }

    /** 品类选项与推荐用料 */
    @GetMapping("/meta")
    public ApiResponse<Map<String, Object>> meta() {
        Map<String, Object> meta = new java.util.LinkedHashMap<>();
        meta.put("categories", GarmentDict.categories());
        meta.put("defaultMeters", styleService.defaultMeters());
        return ApiResponse.success(meta);
    }
}
