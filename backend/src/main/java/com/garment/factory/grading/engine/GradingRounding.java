package com.garment.factory.grading.engine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

import com.garment.factory.common.BizException;

/**
 * 统一舍入口径（放码行业惯例：四舍五入，即「半数远离 0」）。
 *
 * <p>由<b>量子（quantum）</b>定义：内部单位为微米，量子是结果的最小步进。
 * 预设：
 * <ul>
 *   <li>{@code CM / 0.1} → 1000 μm（毫米级，旧版放码表口径）</li>
 *   <li>{@code CM / 0.5} → 5000 μm</li>
 *   <li>{@code CM / 1}   → 10000 μm</li>
 *   <li>{@code MM / 1}   → 1000 μm</li>
 *   <li>{@code MM / 5}   → 5000 μm</li>
 * </ul>
 * 整组结果在最终输出时统一按一个量子舍入；中间过程绝不舍入，因此换单位不会累计误差。
 */
public final class GradingRounding {

    /** 半进位：0.5 向远离 0 的方向走（四舍五入；负数 -2.5 → -3）。 */
    public static final RoundingMode MODE = RoundingMode.HALF_UP;

    private GradingRounding() {
    }

    /** 由「单位 + 步进」解析量子（微米）。 */
    public static BigDecimal quantumOf(String unit, String step) {
        LengthUnit u = LengthUnit.of(unit);
        BigDecimal stepDec;
        try {
            stepDec = new BigDecimal(step == null || step.isBlank() ? "0.1" : step.trim());
        } catch (NumberFormatException e) {
            throw new BizException("舍入步进不是合法数字：" + step);
        }
        if (stepDec.signum() <= 0) {
            throw new BizException("舍入步进必须大于 0");
        }
        BigDecimal quantum = u.toMicrons(stepDec);
        if (quantum.signum() <= 0 || quantum.stripTrailingZeros().scale() > 0) {
            throw new BizException("舍入量子折算微米后必须为正整数，当前为 " + quantum.toPlainString());
        }
        return quantum;
    }

    /**
     * 把任意微米有理数量舍入到量子的整数倍（半进位，负数对称）。
     */
    public static BigDecimal round(BigDecimal microns, BigDecimal quantum) {
        // n = round(microns / quantum)，再 ×quantum；BigDecimal#divide 支持 HALF_UP（对负数对称远离0）
        BigDecimal multiples = microns.divide(quantum, 0, MODE);
        return multiples.multiply(quantum);
    }

    /**
     * 结果展示时的小数位：量子折算到目标单位后，其步进值的小数位数。
     * 如量子 1000μm → cm 下 0.1（1 位）、mm 下 1（0 位）；
     * 量子 5000μm → cm 下 0.5（1 位）、mm 下 5（0 位）。
     */
    public static int displayDecimals(BigDecimal quantum, LengthUnit unit) {
        BigDecimal step = quantum.divide(unit.micronFactor());
        BigDecimal stripped = step.stripTrailingZeros();
        // 量子已是微米整数，折算后若步进 ≥1 则 scale≤0（无小数位）；步进 0.1 时 scale=1
        return Math.max(0, stripped.scale());
    }

    /** 预设舍入口径，供前端下拉。 */
    public static Map<String, Object> presets() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("cm_0_1", Map.of("unit", "cm", "step", "0.1", "label", "厘米 · 0.1cm（毫米，旧版口径）"));
        map.put("cm_0_5", Map.of("unit", "cm", "step", "0.5", "label", "厘米 · 0.5cm"));
        map.put("cm_1", Map.of("unit", "cm", "step", "1", "label", "厘米 · 1cm"));
        map.put("mm_1", Map.of("unit", "mm", "step", "1", "label", "毫米 · 1mm"));
        map.put("mm_5", Map.of("unit", "mm", "step", "5", "label", "毫米 · 5mm"));
        return map;
    }
}
