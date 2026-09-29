package com.garment.factory.grading.engine;

import java.math.BigDecimal;

/**
 * 一个「部位槽位 × 尺码」的结果单元。
 *
 * @param slotKey          槽位 key
 * @param slotName         槽位展示名
 * @param side             侧别 null/L/R
 * @param grade            级差
 * @param sizeName         尺码名
 * @param rawMicrons       未舍入尺寸（μm 有理数量，供差异分析定位舍入差）
 * @param finalMicrons     统一舍入后的最终尺寸（μm）
 * @param baseDeltaMicrons 相对基准版的<b>总变化</b>（最终尺寸 − 基准最终尺寸，μm）
 */
public record GradingCell(
        String slotKey,
        String slotName,
        String side,
        int grade,
        String sizeName,
        BigDecimal rawMicrons,
        BigDecimal finalMicrons,
        BigDecimal baseDeltaMicrons) {
}
