package com.garment.factory.grading.engine;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.garment.factory.common.BizException;

/**
 * 整组放码生成器：校验方案结构 → 逐槽位逐尺码计算 → 统一舍入 → 全量异常检测。
 *
 * <p>任何结构 / 公式问题在本类即抛 {@link BizException}（<b>先于任何结果产出</b>），
 * 因此调用方保留的上一次结果不会被半成品覆盖。
 */
public final class GradingGenerator {

    private GradingGenerator() {
    }

    public static GradingResultSet generate(GradingSpec spec) {
        validateStructure(spec);

        List<GradingCell> cells = new ArrayList<>(spec.parts().size() * spec.sizes().size());
        Map<String, String> signatures = new LinkedHashMap<>();
        // 槽位 → 该槽位按尺码顺序排好的格子，供异常检测 O(1) 顺序遍历（避免 O(格²) 全表扫描）
        Map<String, List<GradingCell>> cellsBySlot = new LinkedHashMap<>();

        for (GradingPart part : spec.parts()) {
            signatures.put(part.slotKey(), part.rule().getType().signature(part.rule()));
            BigDecimal rawBase = GradingEngine.rawAt(part, spec.baseGrade());
            BigDecimal finalBase = GradingRounding.round(rawBase, spec.quantumMicrons());

            List<GradingCell> slotCells = new ArrayList<>(spec.sizes().size());
            for (GradingSpec.SizeGrade size : spec.sizes()) {
                BigDecimal raw = GradingEngine.rawAt(part, size.grade());
                BigDecimal rounded = GradingRounding.round(raw, spec.quantumMicrons());
                BigDecimal delta = rounded.subtract(finalBase);
                GradingCell cell = new GradingCell(part.slotKey(), part.displayName(), part.side(),
                        size.grade(), size.name(), raw, rounded, delta);
                cells.add(cell);
                slotCells.add(cell);
            }
            cellsBySlot.put(part.slotKey(), slotCells);
        }

        List<GradingAnomaly> anomalies = new ArrayList<>();
        detectValueAnomalies(spec, cells, anomalies);
        detectDirectionAnomalies(spec, cellsBySlot, anomalies);
        detectRatioAnomalies(spec, cellsBySlot, anomalies);

        return new GradingResultSet(spec.quantumMicrons(), spec.baseGrade(),
                spec.sizes(), spec.parts(), List.copyOf(cells), List.copyOf(anomalies), signatures);
    }

    /* ------------------------------------------------------------------ 结构校验 */

    private static void validateStructure(GradingSpec spec) {
        if (spec.sizes() == null || spec.sizes().isEmpty()) {
            throw new BizException("目标尺码序列不能为空");
        }
        if (spec.sizes().stream().noneMatch(s -> s.grade() == spec.baseGrade())) {
            throw new BizException("基准级差 " + spec.baseGrade() + " 不在目标尺码序列内");
        }
        int prev = Integer.MIN_VALUE;
        for (GradingSpec.SizeGrade s : spec.sizes()) {
            if (s.grade() <= prev) {
                throw new BizException("尺码级差必须严格递增：" + s.grade());
            }
            prev = s.grade();
        }
        if (spec.parts() == null || spec.parts().isEmpty()) {
            throw new BizException("放码部位不能为空");
        }
        Map<String, GradingPart> seen = new LinkedHashMap<>();
        for (GradingPart p : spec.parts()) {
            if (seen.put(p.slotKey(), p) != null) {
                throw new BizException("部位槽位重复：" + p.slotKey());
            }
            if (p.baseMicrons() == null) {
                throw new BizException("部位「" + p.displayName() + "」缺少基准尺寸");
            }
            if (p.rule() == null || p.rule().getType() == null) {
                throw new BizException("部位「" + p.displayName() + "」缺少放码规则");
            }
            validateSegmentCoverage(p, spec);
        }
        for (RatioCheck check : spec.ratioChecks()) {
            if (seen.get(check.numeratorSlot()) == null) {
                throw new BizException("比例检查「" + check.label() + "」的分子部位不存在："
                        + check.numeratorSlot());
            }
            if (seen.get(check.denominatorSlot()) == null) {
                throw new BizException("比例检查「" + check.label() + "」的分母部位不存在："
                        + check.denominatorSlot());
            }
            if (check.minRatio().signum() <= 0 || check.maxRatio().compareTo(check.minRatio()) <= 0) {
                throw new BizException("比例检查「" + check.label() + "」的区间必须为 0 < 下限 < 上限");
            }
        }
    }

    /** 分段规则：所有经过的边都必须被恰好一个区段覆盖（不缺不叠）；校验后建立 O(1) 边索引。 */
    private static void validateSegmentCoverage(GradingPart part, GradingSpec spec) {
        if (part.rule().getType() != RuleType.SEGMENT) {
            return;
        }
        List<Segment> segs = part.rule().getSegments();
        if (segs.isEmpty()) {
            throw new BizException("部位「" + part.displayName() + "」分段规则没有任何区段");
        }
        int minEdge = spec.minGrade();                 // 最低边（连接 minGrade 与 minGrade+1）
        int maxEdge = spec.maxGrade() - 1;             // 最高边（连接 maxGrade-1 与 maxGrade）
        Map<Integer, BigDecimal> index = new LinkedHashMap<>();
        for (int edge = minEdge; edge <= maxEdge; edge++) {
            int covers = 0;
            for (Segment seg : segs) {
                if (seg.getToGrade() <= seg.getFromGrade()) {
                    throw new BizException("部位「" + part.displayName() + "」区段 "
                            + seg.getFromGrade() + ".." + seg.getToGrade()
                            + " 非法：结束级差必须大于起始级差");
                }
                if (seg.coversEdge(edge)) {
                    covers++;
                    if (seg.getDeltaMicrons() == null) {
                        throw new BizException("部位「" + part.displayName() + "」区段 "
                                + seg.getFromGrade() + ".." + seg.getToGrade() + " 缺少变化量");
                    }
                    index.put(edge, seg.getDeltaMicrons());
                }
            }
            if (covers == 0) {
                throw new BizException("部位「" + part.displayName() + "」分段规则不完整：缺少边 "
                        + edge + "→" + (edge + 1) + "（" + spec.sizeName(edge) + " → "
                        + spec.sizeName(edge + 1) + "）的增量");
            }
            if (covers > 1) {
                throw new BizException("部位「" + part.displayName() + "」分段规则在边 "
                        + edge + "→" + (edge + 1) + " 上存在重叠区段");
            }
        }
        part.rule().setEdgeDeltaIndex(index);
    }

    /* ------------------------------------------------------------------ 异常检测 */

    /** 负值 / 零长度。 */
    private static void detectValueAnomalies(GradingSpec spec, List<GradingCell> cells,
                                             List<GradingAnomaly> anomalies) {
        for (GradingCell c : cells) {
            int cmp = c.finalMicrons().signum();
            if (cmp < 0) {
                anomalies.add(new GradingAnomaly(AnomalyType.NEGATIVE, c.grade(), c.sizeName(),
                        c.slotKey(), c.slotName(), null, null,
                        "最终尺寸为负（" + micronText(c.finalMicrons()) + "），纸样无意义"));
            } else if (cmp == 0) {
                anomalies.add(new GradingAnomaly(AnomalyType.ZERO_LENGTH, c.grade(), c.sizeName(),
                        c.slotKey(), c.slotName(), null, null,
                        "最终尺寸为零长度，纸样不可用"));
            }
        }
    }

    /**
     * 增量方向错误 / 相邻倒挂。
     * <ul>
     *   <li>方向错误看<b>规则边本身的变化量符号</b>（未舍入），如 UP 部位配置了负增量；</li>
     *   <li>倒挂看<b>相邻尺码的最终尺寸顺序</b>（舍入后），按声明方向判定。</li>
     * </ul>
     */
    private static void detectDirectionAnomalies(GradingSpec spec,
                                                 Map<String, List<GradingCell>> cellsBySlot,
                                                 List<GradingAnomaly> anomalies) {
        for (GradingPart part : spec.parts()) {
            Direction dir = part.direction() == null ? Direction.ANY : part.direction();

            // 1) 逐条边检查规则增量符号（与实际尺码边界相交的边才检查）
            int minEdge = spec.minGrade();
            int maxEdge = spec.maxGrade() - 1;
            for (int edge = minEdge; edge <= maxEdge; edge++) {
                // 边 edge 两端的未舍入尺寸之差，即规则在该边上的真实变化量
                BigDecimal lo = GradingEngine.rawAt(part, edge);
                BigDecimal hi = GradingEngine.rawAt(part, edge + 1);
                BigDecimal edgeDelta = hi.subtract(lo);
                if (dir.isWrongSign(edgeDelta.signum())) {
                    anomalies.add(new GradingAnomaly(AnomalyType.INCREMENT_DIRECTION, edge + 1,
                            spec.sizeName(edge + 1), part.slotKey(), part.displayName(),
                            edge, edge + 1,
                            "增量方向错误：该部位声明为" + dirName(dir) + "，但 "
                                    + spec.sizeName(edge) + "→" + spec.sizeName(edge + 1)
                                    + " 的单级变化量为 " + signedText(edgeDelta)));
                }
            }

            // 2) 相邻尺码最终尺寸倒挂（含舍入后的平台不算倒挂）
            if (dir == Direction.ANY) {
                continue;
            }
            GradingCell prev = null;
            for (GradingCell cur : cellsBySlot.getOrDefault(part.slotKey(), List.of())) {
                if (prev != null && dir.isWrongOrder(cur.finalMicrons().compareTo(prev.finalMicrons()))) {
                    anomalies.add(new GradingAnomaly(AnomalyType.INVERSION, cur.grade(), cur.sizeName(),
                            part.slotKey(), part.displayName(), prev.grade(), cur.grade(),
                            "相邻尺码倒挂：" + prev.sizeName() + " 为 " + micronText(prev.finalMicrons())
                                    + "，" + cur.sizeName() + " 为 " + micronText(cur.finalMicrons())
                                    + "，与" + dirName(dir) + "方向矛盾"));
                }
                prev = cur;
            }
        }
    }

    /** 肩宽/胸围等跨部位比例（按最终尺寸，定位到尺码）。 */
    private static void detectRatioAnomalies(GradingSpec spec,
                                             Map<String, List<GradingCell>> cellsBySlot,
                                             List<GradingAnomaly> anomalies) {
        for (RatioCheck check : spec.ratioChecks()) {
            List<GradingCell> numerators = cellsBySlot.get(check.numeratorSlot());
            List<GradingCell> denominators = cellsBySlot.get(check.denominatorSlot());
            if (numerators == null || denominators == null) {
                continue;
            }
            Map<Integer, GradingCell> denomByGrade = new LinkedHashMap<>();
            denominators.forEach(c -> denomByGrade.put(c.grade(), c));
            for (GradingCell numerator : numerators) {
                GradingCell denominator = denomByGrade.get(numerator.grade());
                if (denominator == null || denominator.finalMicrons().signum() == 0) {
                    continue;
                }
                BigDecimal ratio = numerator.finalMicrons()
                        .divide(denominator.finalMicrons(), 6, java.math.RoundingMode.HALF_UP);
                if (ratio.compareTo(check.minRatio()) < 0 || ratio.compareTo(check.maxRatio()) > 0) {
                    anomalies.add(new GradingAnomaly(AnomalyType.RATIO_ABNORMAL, numerator.grade(),
                            numerator.sizeName(), numerator.slotKey(), numerator.slotName(), null, null,
                            check.label() + "异常：" + numerator.slotName() + " "
                                    + micronText(numerator.finalMicrons()) + " ÷ 对照部位 "
                                    + micronText(denominator.finalMicrons()) + " = "
                                    + ratio.stripTrailingZeros().toPlainString()
                                    + "，超出合理区间 ["
                                    + check.minRatio().stripTrailingZeros().toPlainString() + ", "
                                    + check.maxRatio().stripTrailingZeros().toPlainString() + "]"));
                }
            }
        }
    }

    private static String dirName(Direction dir) {
        return dir == Direction.UP ? "升码增大" : dir == Direction.DOWN ? "升码减小" : "方向不限";
    }

    private static String micronText(BigDecimal microns) {
        // 对外描述统一用 cm（μm ÷ 10000），保留真实小数
        return microns.divide(LengthUnit.CM.micronFactor()).stripTrailingZeros().toPlainString() + "cm";
    }

    private static String signedText(BigDecimal microns) {
        String t = micronText(microns.abs());
        return microns.signum() < 0 ? "−" + t : "+" + t;
    }
}
