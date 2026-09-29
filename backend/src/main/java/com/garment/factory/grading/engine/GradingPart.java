package com.garment.factory.grading.engine;

import java.math.BigDecimal;

/**
 * 放码部位槽位：一个部位的一个「侧」。
 *
 * <ul>
 *   <li>对称部位（胸围、腰围、衣长…）只有 side = null 一个槽位；</li>
 *   <li>左右不对称部位（肩宽、袖长等按人体左右分别放码时）拆成 L / R 两个独立槽位，
 *       各自基准值与规则分别计算，<b>不自动合并、不互相平均</b>。</li>
 * </ul>
 *
 * @param partId       部位编码，如 bust / shoulder
 * @param partName     部位中文名
 * @param side         侧别：null=整体（对称），L=左，R=右
 * @param baseMicrons  基准尺码下的尺寸（μm）
 * @param direction    期望放码方向：UP 升码只允许不减 / DOWN / ANY 不限
 * @param rule         放码规则
 */
public record GradingPart(
        String partId,
        String partName,
        String side,
        BigDecimal baseMicrons,
        Direction direction,
        RuleConfig rule) {

    /** 槽位唯一键：部位 + 侧。 */
    public String slotKey() {
        return side == null ? partId : partId + "#" + side;
    }

    /** 展示名：肩宽(左)。 */
    public String displayName() {
        if (side == null) {
            return partName;
        }
        return partName + "(" + ("L".equals(side) ? "左" : "右") + ")";
    }
}
