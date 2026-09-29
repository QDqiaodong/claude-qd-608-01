package com.garment.factory.grading.engine;

import com.garment.factory.common.BizException;

/**
 * 部位期望的放码方向（用于异常检测）：
 * <ul>
 *   <li>{@link #UP} 升码应增大（三围、衣长等）；增量为负即「增量方向错误」；</li>
 *   <li>{@link #DOWN} 升码应减小；</li>
 *   <li>{@link #ANY} 方向不限（如某些版型微调部位），不做方向检查。</li>
 * </ul>
 */
public enum Direction {
    UP,
    DOWN,
    ANY;

    public static Direction of(String code) {
        if (code == null || code.isBlank()) {
            return ANY;
        }
        return switch (code.trim().toUpperCase()) {
            case "UP" -> UP;
            case "DOWN" -> DOWN;
            case "ANY" -> ANY;
            default -> throw new BizException("放码方向只能是 UP / DOWN / ANY：" + code);
        };
    }

    /** 相邻尺码最终尺寸倒挂时的判定方向。 */
    public boolean isWrongOrder(int cmpResult) {
        // cmpResult = next.compareTo(prev)
        return switch (this) {
            case UP -> cmpResult < 0;   // 该升却变小 = 倒挂
            case DOWN -> cmpResult > 0; // 该降却变大 = 倒挂
            case ANY -> false;
        };
    }

    /** 单条边的增量符号是否与期望方向冲突（零增量不算冲突，属允许的停码）。 */
    public boolean isWrongSign(int signum) {
        return switch (this) {
            case UP -> signum < 0;
            case DOWN -> signum > 0;
            case ANY -> false;
        };
    }
}
