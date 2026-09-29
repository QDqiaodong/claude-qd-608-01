package com.garment.factory.grading.engine;

import java.math.BigDecimal;

/**
 * 跨部位比例合理性检查。默认场景：肩宽 ÷ 胸围 应落在 [minRatio, maxRatio]，
 * 明显越界（如 0.90 或 0.10）报「比例异常」，定位到尺码与具体槽位。
 *
 * <p>不对称时按同一侧配对（左肩宽 ÷ 胸围；胸围为整体槽位），左右各自独立检查、独立报错。
 *
 * @param checkCode   检查编码，如 SHOULDER_CHEST
 * @param numeratorSlot   分子槽位 key（如 shoulder / shoulder#L）
 * @param denominatorSlot 分母槽位 key（如 bust）
 * @param minRatio    允许下限（含）
 * @param maxRatio    允许上限（含）
 * @param label       检查项中文名
 */
public record RatioCheck(
        String checkCode,
        String numeratorSlot,
        String denominatorSlot,
        BigDecimal minRatio,
        BigDecimal maxRatio,
        String label) {
}
