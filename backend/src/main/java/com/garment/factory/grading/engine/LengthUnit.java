package com.garment.factory.grading.engine;

import java.math.BigDecimal;

import com.garment.factory.common.BizException;

/**
 * 长度单位与内部统一口径。
 *
 * <p>放码引擎的<b>唯一内部口径是「微米」的有理数量（BigDecimal 个）</b>：
 * <ul>
 *   <li>1 cm = 10 000 μm（精确）</li>
 *   <li>1 mm = 1 000 μm（精确）</li>
 * </ul>
 * 厘米与毫米录入只是同一内部量的不同写法，{@code ×10000} / {@code ×1000} 都是整数倍，
 * 因此单位来回切换只改显示、不再舍入，从结构上消除累计误差与逐步漂移。
 *
 * <p>对外显示时，微米整数可无损换算为 cm（÷10000）或 mm（÷1000）。
 */
public enum LengthUnit {

    CM(BigDecimal.valueOf(10_000)),
    MM(BigDecimal.valueOf(1_000));

    /** 该单位 1 个单位对应多少微米。 */
    private final BigDecimal micronFactor;

    LengthUnit(BigDecimal micronFactor) {
        this.micronFactor = micronFactor;
    }

    public BigDecimal micronFactor() {
        return micronFactor;
    }

    /** 把以本单位表示的十进制数值，精确换算成微米有理数量。 */
    public BigDecimal toMicrons(BigDecimal value) {
        if (value == null) {
            throw new BizException("长度数值不能为空");
        }
        return value.multiply(micronFactor);
    }

    /** 把微米有理数量换算回本单位（除法除不尽时按 given mc 保留，但展示前调用方一般已在量子上取整）。 */
    public BigDecimal fromMicrons(BigDecimal microns, java.math.MathContext mc) {
        return microns.divide(micronFactor, mc);
    }

    public static LengthUnit of(String code) {
        if (code == null) {
            return CM;
        }
        return switch (code.trim().toUpperCase()) {
            case "CM" -> CM;
            case "MM" -> MM;
            default -> throw new BizException("长度单位只能是 cm 或 mm：" + code);
        };
    }
}
