package com.garment.factory.grading.engine;

/**
 * 一条定位后的异常说明。
 *
 * @param type      异常类型
 * @param grade     级差
 * @param sizeName  尺码名（如 M / 165/88A）
 * @param slotKey   部位槽位 key
 * @param slotName  部位槽位展示名（如 肩宽(左)）
 * @param edgeFrom  若是相邻级问题，起始级差；否则为 null
 * @param edgeTo    若是相邻级问题，到达级差；否则为 null
 * @param reason    中文原因，含实际数值
 */
public record GradingAnomaly(
        AnomalyType type,
        int grade,
        String sizeName,
        String slotKey,
        String slotName,
        Integer edgeFrom,
        Integer edgeTo,
        String reason) {

    public static GradingAnomaly of(AnomalyType type, int grade, String sizeName,
                                    GradingPart part, String reason) {
        return new GradingAnomaly(type, grade, sizeName, part.slotKey(), part.displayName(),
                null, null, reason);
    }
}
