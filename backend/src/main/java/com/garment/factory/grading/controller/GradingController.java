package com.garment.factory.grading.controller;

import java.util.LinkedHashMap;
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
import com.garment.factory.grading.doc.SchemeDoc;
import com.garment.factory.grading.dto.SchemeSaveReq;
import com.garment.factory.grading.engine.GradingRounding;
import com.garment.factory.grading.model.GradingScheme;
import com.garment.factory.grading.service.GradingSchemeService;

/**
 * 版型批量放码：方案 / 生成 / 临时计算 / 两方案比较。
 *
 * <p>全部计算规则、精度口径、结果完整性都在服务端统一保证；前端仅做录入与展示，
 * 切 cm/mm 只改渲染（结果中两套显示值由微米精确换算），浏览器里不做任何放码运算。
 */
@RestController
@RequestMapping("/api/grading")
public class GradingController {

    private final GradingSchemeService service;

    public GradingController(GradingSchemeService service) {
        this.service = service;
    }

    /** 方案列表（默认只看留存方案，temporary=true 看临时方案）。 */
    @GetMapping("/schemes")
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(required = false) Boolean temporary,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ApiResponse.success(service.list(temporary, keyword, page, size));
    }

    @GetMapping("/schemes/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.success(service.detail(id));
    }

    /** 仅留存方案（结构校验通过即保存；参数改动后旧结果标记 stale，不会冒充新结果）。 */
    @PostMapping("/schemes")
    public ApiResponse<GradingScheme> save(@RequestBody SchemeSaveReq req) {
        GradingScheme saved = service.save(req.getId(), req.getSchemeNo(), req.getName(),
                req.getStyleId(), Boolean.TRUE.equals(req.getTemporary()), req.getRemark(), req.getSpec());
        return ApiResponse.success("放码方案已留存", saved);
    }

    /** 保存并立即生成：要么拿到整套新结果，要么整体失败、上一次完整结果保留。 */
    @PostMapping("/schemes/save-and-generate")
    public ApiResponse<Map<String, Object>> saveAndGenerate(@RequestBody SchemeSaveReq req) {
        return ApiResponse.success("已留存并生成完整放码结果",
                service.saveAndGenerate(req.getId(), req.getSchemeNo(), req.getName(), req.getStyleId(),
                        Boolean.TRUE.equals(req.getTemporary()), req.getRemark(), req.getSpec()));
    }

    /** 对已留存方案重新生成整组结果（成功才覆盖，失败保留旧结果）。 */
    @PostMapping("/schemes/{id}/generate")
    public ApiResponse<Map<String, Object>> generate(@PathVariable Long id) {
        return ApiResponse.success("整组放码结果已重新生成", Map.of("result", service.generate(id)));
    }

    @DeleteMapping("/schemes/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.success("已删除放码方案", null);
    }

    /**
     * 临时计算方案：参数直接进引擎，不落库、不影响任何已留存方案。
     * 每次请求独立，绝不复用上一次对象，因此不会残留上一次结果。
     */
    @PostMapping("/preview")
    public ApiResponse<Map<String, Object>> preview(@RequestBody SchemeDoc doc) {
        return ApiResponse.success(service.preview(doc));
    }

    /** 两套放码方案逐部位比较（公式差异 / 舍入差异 / 最终尺寸差异）。 */
    @GetMapping("/compare")
    public ApiResponse<Map<String, Object>> compare(@RequestParam Long schemeAId,
                                                    @RequestParam Long schemeBId) {
        return ApiResponse.success(service.compare(schemeAId, schemeBId));
    }

    /** 规则类型、方向、舍入预设等元数据。 */
    @GetMapping("/meta")
    public ApiResponse<Map<String, Object>> meta() {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("ruleTypes", new String[]{"FIXED", "RATIO", "SEGMENT"});
        meta.put("directions", new String[]{"UP", "DOWN", "ANY"});
        meta.put("units", new String[]{"cm", "mm"});
        meta.put("roundingPresets", GradingRounding.presets());
        return ApiResponse.success(meta);
    }
}
