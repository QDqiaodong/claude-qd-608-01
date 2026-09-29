package com.garment.factory.grading.engine;

import java.math.BigDecimal;
import java.math.MathContext;

import com.garment.factory.common.BizException;

/**
 * 十进制解析 / 展示：所有外部长度都以<b>字符串</b>进出，避免 double / float 二进制误差。
 *
 * <ul>
 *   <li>{@link #parseLength(String, LengthUnit)}：把 "96.5" cm / "965" mm 精确折成微米；</li>
 *   <li>{@link #toDisplay(BigDecimal, LengthUnit, int)}：把已经在舍入量子上取整的微米值，
 *       无损格式化成目标单位的字符串（单位换算本身是整除，不产生新舍入）。</li>
 * </ul>
 */
public final class DecimalQuantity {

    /** 引擎中间运算的安全精度（仅用于除法兜底；正常公式 × 幂均为有限小数，实际用不到截断）。 */
    public static final MathContext ENGINE_MC = new MathContext(60, java.math.RoundingMode.HALF_UP);

    private DecimalQuantity() {
    }

    /**
     * 解析长度字符串。要求是十进制（可含小数点、正负号），按输入单位精确折算微米。
     * 毫米模式输入 "900.5" 表示 900.5mm（=90.05cm），也允许一位小数，折算是精确的。
     */
    public static BigDecimal parseLength(String text, LengthUnit unit) {
        if (text == null || text.isBlank()) {
            throw new BizException("长度数值不能为空");
        }
        String s = text.trim().replace(",", "");
        BigDecimal value;
        try {
            value = new BigDecimal(s);
        } catch (NumberFormatException e) {
            throw new BizException("长度不是合法数字：「" + text + "」");
        }
        return unit.toMicrons(value);
    }

    /**
     * 解析变化量：变化量可以为负（降码），与长度同口径（内部微米）。
     */
    public static BigDecimal parseDelta(String text, LengthUnit unit) {
        return parseLength(text, unit);
    }

    /**
     * 解析比例百分比：既接受 "4%" 也接受 "0.04"（小数比例）。
     * 结果是无量纲比例（4% → 0.04），用 BigDecimal 精确表达有限小数。
     */
    public static BigDecimal parseRate(String text) {
        if (text == null || text.isBlank()) {
            throw new BizException("比例数值不能为空");
        }
        String s = text.trim();
        boolean percent = s.endsWith("%");
        if (percent) {
            s = s.substring(0, s.length() - 1).trim();
        }
        BigDecimal value;
        try {
            value = new BigDecimal(s);
        } catch (NumberFormatException e) {
            throw new BizException("比例不是合法数字：「" + text + "」");
        }
        if (percent) {
            // value% = value / 100
            value = value.divide(BigDecimal.valueOf(100), ENGINE_MC);
        }
        return value;
    }

    /**
     * 把微米值按目标单位展示。{@code microns} 已经落在舍入量子的整数倍上，
     * 所以 cm(÷10000)/mm(÷1000) 都是精确整除，<b>这里不做任何舍入</b>。
     *
     * @param decimals 期望保留的小数位（仅用于控制补零展示，不改变数值）
     */
    public static String toDisplay(BigDecimal microns, LengthUnit unit, int decimals) {
        BigDecimal divided = microns.divide(unit.micronFactor());
        if (decimals < 0) {
            // 自动：去掉末尾多余的 0
            return divided.stripTrailingZeros().toPlainString();
        }
        return divided.setScale(decimals, java.math.RoundingMode.UNNECESSARY).toPlainString();
    }

    /** 把微米值转成目标单位的 BigDecimal（精确整除时使用）。 */
    public static BigDecimal toUnitValue(BigDecimal microns, LengthUnit unit) {
        return microns.divide(unit.micronFactor());
    }
}
