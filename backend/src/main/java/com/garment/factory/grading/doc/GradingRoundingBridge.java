package com.garment.factory.grading.doc;

import java.math.BigDecimal;

import com.garment.factory.grading.engine.GradingRounding;

/**
 * 文档层与引擎层的舍入口径桥接。
 */
final class GradingRoundingBridge {

    private GradingRoundingBridge() {
    }

    static BigDecimal quantum(SchemeDoc.RoundingDoc rounding) {
        return GradingRounding.quantumOf(rounding.getUnit(), rounding.getStep());
    }
}
