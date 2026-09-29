package com.garment.factory.grading.doc;

import java.math.BigDecimal;

import com.garment.factory.common.BizException;

/**
 * 旧版（v1）放码方案兼容读入。
 *
 * <p>v1 只有厘米小数、固定增量（每级同一个 cm 值），无单位字段、无舍入配置。
 * 兼容规则（固定、可复验）：
 * <ol>
 *   <li>所有数值一律按<b>厘米</b>解析（mm 模式是 v2 才有的概念，绝不把旧数当毫米放大 10 倍）；</li>
 *   <li>舍入口径固定为 <b>cm / 0.1（四舍五入到毫米）</b>——旧放码表的行业惯例口径；</li>
 *   <li>迁移只在内存进行，换算全部精确（cm→μm 为 ×10000），因此 v1 重新生成的每个数值
 *       都与旧版「厘米小数直接计算再保留 1 位」的结果完全一致；</li>
 *   <li>一旦版师另存为当前版本，才写成 schemaVersion=2；原 v1 记录不动。</li>
 * </ol>
 */
public final class LegacySchemeMigrator {

    private LegacySchemeMigrator() {
    }

    /**
     * v1 文档形如：
     * <pre>
     * { "baseSize":"M",
     *   "sizes":[{"name":"S","grade":-1},{"name":"M","grade":0},{"name":"L","grade":1}],
     *   "parts":[
     *     {"partId":"bust","partName":"胸围","base":"90.0","delta":"4.0","direction":"UP"}
     *   ] }
     * </pre>
     * grade 缺省时按 sizes 顺序居中推导。
     */
    public static SchemeDoc migrate(
            com.fasterxml.jackson.databind.JsonNode root,
            com.fasterxml.jackson.databind.ObjectMapper mapper) {
        SchemeDoc doc = new SchemeDoc();
        doc.setSchemaVersion(2); // 内存中按 v2 计算；存储是否升级由调用方决定
        doc.setInputUnit("cm");
        SchemeDoc.RoundingDoc rounding = new SchemeDoc.RoundingDoc();
        rounding.setUnit("cm");
        rounding.setStep("0.1");
        doc.setRounding(rounding);
        doc.setBaseGrade(0);

        var sizeNodes = root.path("sizes");
        if (!sizeNodes.isArray() || sizeNodes.isEmpty()) {
            throw new BizException("旧版方案缺少尺码序列 sizes");
        }
        java.util.List<SchemeDoc.SizeDoc> sizes = new java.util.ArrayList<>();
        int idx = 0;
        int n = sizeNodes.size();
        // 居中推导：n 个尺码，基准放中间；奇数自然居中，偶数时基准偏右与行业习惯一致
        int autoBase = -((n - 1) / 2);
        for (com.fasterxml.jackson.databind.JsonNode sn : sizeNodes) {
            SchemeDoc.SizeDoc size = new SchemeDoc.SizeDoc();
            size.setName(sn.path("name").asText("尺码" + (idx + 1)));
            int grade = sn.has("grade") && sn.get("grade").isNumber()
                    ? sn.get("grade").asInt() : autoBase + idx;
            size.setGrade(grade);
            sizes.add(size);
            idx++;
        }
        // 基准：显式 baseSize 名称匹配，否则 grade=0，再否则居中
        String baseSizeName = root.path("baseSize").asText(null);
        int baseGrade = 0;
        if (baseSizeName != null) {
            int found = Integer.MIN_VALUE;
            for (SchemeDoc.SizeDoc s : sizes) {
                if (baseSizeName.equals(s.getName())) {
                    found = s.getGrade();
                    break;
                }
            }
            if (found != Integer.MIN_VALUE) {
                baseGrade = found;
            }
        } else if (sizes.stream().noneMatch(s -> s.getGrade() == 0)) {
            baseGrade = sizes.get((n - 1) / 2).getGrade();
        }
        doc.setBaseGrade(baseGrade);
        doc.setSizes(sizes);

        var partNodes = root.path("parts");
        if (!partNodes.isArray()) {
            throw new BizException("旧版方案缺少部位定义 parts");
        }
        java.util.List<SchemeDoc.PartDoc> parts = new java.util.ArrayList<>();
        for (com.fasterxml.jackson.databind.JsonNode pn : partNodes) {
            SchemeDoc.PartDoc part = new SchemeDoc.PartDoc();
            part.setPartId(textOrThrow(pn, "partId", "旧版部位缺少 partId"));
            part.setPartName(pn.path("partName").asText(part.getPartId()));
            part.setSide(pn.has("side") && !pn.get("side").isNull() ? pn.get("side").asText() : null);
            part.setUnit("cm");
            part.setDirection(pn.path("direction").asText("ANY"));
            // 校验旧数值是合法十进制（按 cm）
            String base = textOrThrow(pn, "base", "旧版部位「" + part.getPartId() + "」缺少 base");
            parseLegacyDecimal(base, "base");
            part.setBase(base);

            SchemeDoc.RuleDoc rule = new SchemeDoc.RuleDoc();
            String delta = textOrThrow(pn, "delta", "旧版部位「" + part.getPartId() + "」缺少 delta");
            parseLegacyDecimal(delta, "delta");
            rule.setType("FIXED");
            rule.setDelta(delta);
            rule.setUnit("cm");
            part.setRule(rule);
            parts.add(part);
        }
        doc.setParts(parts);
        return doc;
    }

    private static String textOrThrow(com.fasterxml.jackson.databind.JsonNode node, String field, String msg) {
        com.fasterxml.jackson.databind.JsonNode v = node.get(field);
        if (v == null || v.isNull() || v.asText("").isBlank()) {
            throw new BizException(msg);
        }
        return v.asText().trim();
    }

    private static BigDecimal parseLegacyDecimal(String s, String field) {
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            throw new BizException("旧版数值 " + field + " 不是合法厘米小数：" + s);
        }
    }
}
