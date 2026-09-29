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
import com.garment.factory.dto.GradeCompareResult;
import com.garment.factory.dto.GradeResult;
import com.garment.factory.dto.GradeSchemeDetailDto;
import com.garment.factory.dto.GradeSchemeListItemDto;
import com.garment.factory.dto.GradeSchemeReq;
import com.garment.factory.service.GradeSchemeService;

/**
 * 版型批量放码：
 *   规则、精度口径、完整结果全部在服务端计算；
 *   /preview 为不落库临时计算，保存后重开读取的是服务端留存的不可变快照。
 */
@RestController
@RequestMapping("/api/grade-schemes")
public class GradeSchemeController {

    private final GradeSchemeService service;

    public GradeSchemeController(GradeSchemeService service) {
        this.service = service;
    }

    @GetMapping("/meta")
    public ApiResponse<java.util.Map<String, Object>> meta() {
        return ApiResponse.success(service.meta());
    }

    /** 临时计算：整组结果一次性返回，不写库 */
    @PostMapping("/preview")
    public ApiResponse<GradeResult> preview(@RequestBody GradeSchemeReq req) {
        return ApiResponse.success(service.preview(req));
    }

    @GetMapping
    public ApiResponse<List<GradeSchemeListItemDto>> list() {
        return ApiResponse.success(service.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<GradeSchemeDetailDto> detail(@PathVariable Long id) {
        return ApiResponse.success(service.detail(id));
    }

    /** 留存/改版：整组算完才整体替换旧快照 */
    @PostMapping
    public ApiResponse<GradeSchemeDetailDto> save(@RequestBody GradeSchemeReq req) {
        return ApiResponse.success("放码方案已留存，结果已由服务端统一计算并固化", service.save(req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> remove(@PathVariable Long id) {
        service.remove(id);
        return ApiResponse.success("已删除放码方案", null);
    }

    /** 两套方案逐部位比较：公式差异 / 舍入差异 / 最终尺寸差异 */
    @GetMapping("/compare")
    public ApiResponse<GradeCompareResult> compare(@RequestParam Long schemeAId,
                                                   @RequestParam Long schemeBId) {
        return ApiResponse.success(service.compare(schemeAId, schemeBId));
    }
}
