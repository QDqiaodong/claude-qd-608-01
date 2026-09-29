package com.garment.factory.grading.engine;

import java.math.BigDecimal;
import java.util.List;

import com.garment.factory.common.BizException;

/**
 * 放码核心计算：纯函数、确定性、无状态。
 *
 * <p>口径保证：
 * <ul>
 *   <li>全部中间量保留为微米有理数量（BigDecimal，安全精度 60 位），<b>只在输出前统一舍入一次</b>；</li>
 *   <li>比例规则按 (1+rate)^grade 连乘，不逐级取整，杜绝累计误差；</li>
 *   <li>同一份方案多次生成结果完全一致（不含时间戳/随机量）；</li>
 *   <li>结构/公式错误（分段缺口、比例非法等）抛 {@link BizException}，由上层保证整次生成不落库、
 *       上一次完整结果原样保留。</li>
 * </ul>
 */
public final class GradingEngine {

    private GradingEngine() {
    }

    /**
     * 计算单个槽位在某个级差下的<b>未舍入</b>尺寸（μm）。
     */
    public static BigDecimal rawAt(GradingPart part, int grade) {
        BigDecimal base = part.baseMicrons();
        RuleConfig rule = part.rule();
        switch (rule.getType()) {
            case FIXED: {
                if (rule.getDeltaMicrons() == null) {
                    throw new BizException("部位「" + part.displayName() + "」固定增量规则缺少变化量");
                }
                return base.add(rule.getDeltaMicrons().multiply(BigDecimal.valueOf(grade)));
            }
            case RATIO: {
                if (rule.getRate() == null) {
                    throw new BizException("部位「" + part.displayName() + "」比例规则缺少比例");
                }
                BigDecimal factor = BigDecimal.ONE.add(rule.getRate());
                if (factor.signum() <= 0) {
                    throw new BizException("部位「" + part.displayName()
                            + "」比例因子必须为正（1 + 比例 > 0），当前 1 + "
                            + rule.getRate().toPlainString());
                }
                // factor^grade：正整数次自乘；负次为其倒数；BigDecimal pow 不接受负指数
                if (grade == 0) {
                    return base;
                }
                int exp = Math.abs(grade);
                BigDecimal positive = integerPow(factor, exp);
                if (grade < 0) {
                    return base.divide(positive, DecimalQuantity.ENGINE_MC);
                }
                return base.multiply(positive);
            }
            case SEGMENT: {
                List<Segment> segments = rule.getSegments();
                if (segments == null || segments.isEmpty()) {
                    throw new BizException("部位「" + part.displayName() + "」分段规则没有任何区段");
                }
                if (grade == 0) {
                    return base;
                }
                BigDecimal total = base;
                if (grade > 0) {
                    for (int edge = 0; edge < grade; edge++) {
                        total = total.add(deltaOfEdge(part, rule, edge));
                    }
                } else {
                    for (int edge = -1; edge >= grade; edge--) {
                        // 降码经过边 edge（edge 连接 edge 与 edge+1，从 edge+1 退回 edge）
                        total = total.subtract(deltaOfEdge(part, rule, edge));
                    }
                }
                return total;
            }
            default:
                throw new BizException("未知放码规则类型");
        }
    }

    private static BigDecimal deltaOfEdge(GradingPart part, RuleConfig rule, int edge) {
        java.util.Map<Integer, BigDecimal> index = rule.getEdgeDeltaIndex();
        if (index != null) {
            BigDecimal delta = index.get(edge);
            if (delta == null) {
                throw new BizException("部位「" + part.displayName() + "」分段规则缺少边 "
                        + edge + "→" + (edge + 1) + " 的增量定义（区段不连续）");
            }
            return delta;
        }
        // 兜底线性扫描（未经 GradingGenerator 预索引时使用）
        Segment hit = null;
        for (Segment seg : rule.getSegments()) {
            if (seg.coversEdge(edge)) {
                if (hit != null) {
                    throw new BizException("部位「" + part.displayName() + "」分段规则在边 "
                            + edge + "→" + (edge + 1) + " 上有重叠区段");
                }
                hit = seg;
            }
        }
        if (hit == null) {
            throw new BizException("部位「" + part.displayName() + "」分段规则缺少边 "
                    + edge + "→" + (edge + 1) + " 的增量定义（区段不连续）");
        }
        if (hit.getDeltaMicrons() == null) {
            throw new BizException("部位「" + part.displayName() + "」区段 "
                    + hit.getFromGrade() + ".." + hit.getToGrade() + " 缺少变化量");
        }
        return hit.getDeltaMicrons();
    }

    /** 整数次幂（快速幂，全程精确乘法）。 */
    private static BigDecimal integerPow(BigDecimal base, int exponent) {
        BigDecimal result = BigDecimal.ONE;
        BigDecimal b = base;
        int e = exponent;
        while (e > 0) {
            if ((e & 1) == 1) {
                result = result.multiply(b);
            }
            e >>= 1;
            if (e > 0) {
                b = b.multiply(b);
            }
        }
        return result;
    }
}
