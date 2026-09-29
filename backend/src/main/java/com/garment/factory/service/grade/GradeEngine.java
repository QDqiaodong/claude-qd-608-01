package com.garment.factory.service.grade;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.garment.factory.domain.GradeDict;
import com.garment.factory.dto.GradeCell;
import com.garment.factory.dto.GradeIssue;
import com.garment.factory.dto.GradePartResult;
import com.garment.factory.dto.GradeResult;

/**
 * 放码计算引擎（无状态）。
 *
 * 计算口径：
 *   - 全部使用 BigDecimal、单位毫米；比例除法固定 6 位小数 HALF_UP，避免无限小数拖垮；
 *   - 理论值 rawMm 先按级序算出，最终值 valueMm 只按方案舍入步进做一次 HALF_UP；
 *   - 厘米值由 valueMm 精确除以 10 派生（保留与步进对齐的 scale，整除无余），不作为计算输入，
 *     所以厘米/毫米来回切换永远不会二次舍入、不产生累计误差；
 *   - 分段规则按「每一步起点级序所在段的增量」逐级累计，升码累加、降码取相反数；
 *   - 整组结果在一次调用内全部算完，异常关系（非致命）只收集不中断；
 *     结构性无法计算的问题在 GradeNormalizer 阶段直接致命失败，不会走到这里。
 */
@Component
public class GradeEngine {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final int RATIO_SCALE = 6;

    public GradeResult compute(CanonicalScheme scheme) {
        long start = System.nanoTime();
        BigDecimal step = scheme.getRoundStepMm();

        // 先确定每个部位的【舍入后基准】，后续规则以它为起点，保证基准格 deltaMm 恒为 0
        Map<PartSide, BigDecimal> roundedBases = new LinkedHashMap<>();
        for (String part : GradeDict.partKeys()) {
            for (String side : sidesOf(part)) {
                PartSide key = new PartSide(part, side);
                BigDecimal baseRaw = scheme.getBases().get(key);
                roundedBases.put(key, roundToStep(baseRaw, step));
            }
        }

        List<GradePartResult> partResults = new ArrayList<>();
        List<GradeIssue> issues = new ArrayList<>();

        for (String part : GradeDict.partKeys()) {
            for (String side : sidesOf(part)) {
                PartSide key = new PartSide(part, side);
                NormRule rule = scheme.getRules().getOrDefault(key, NormRule.constant(part, side));
                BigDecimal baseRounded = roundedBases.get(key);

                List<GradeCell> cells = new ArrayList<>();
                for (int i = 0; i < scheme.getSizes().size(); i++) {
                    String sizeCode = scheme.getSizes().get(i);
                    int level = scheme.getLevels().get(i);

                    BigDecimal rawMm = rawValue(rule, baseRounded, level);
                    BigDecimal valueMm = roundToStep(rawMm, step);
                    // 厘米只是毫米的精确十进制移位（scale 对齐后整除），不作为计算输入：
                    // 页面在厘米/毫米间切换只是换显示，永远不会触发第二次舍入
                    BigDecimal valueCm = valueMm.divide(BigDecimal.TEN).stripTrailingZeros();
                    BigDecimal deltaMm = valueMm.subtract(baseRounded);
                    cells.add(new GradeCell(sizeCode, level, rawMm, valueMm, valueCm, deltaMm));
                }
                partResults.add(new GradePartResult(part, side, rule.type(),
                        renderFormula(rule, baseRounded), cells));

                collectIssues(issues, part, side, rule, cells);
            }
        }

        collectShoulderRatioIssues(issues, scheme, partResults);

        GradeResult result = new GradeResult();
        result.setBaseSizeCode(scheme.getBaseSizeCode());
        result.setBaseLevel(0);
        result.setInputUnit(scheme.getInputUnit());
        result.setRoundStepMm(step);
        result.setSizes(new ArrayList<>(scheme.getSizes()));
        result.setLevels(new ArrayList<>(scheme.getLevels()));
        result.setParts(partResults);
        result.setIssues(issues);
        result.setElapsedMs((System.nanoTime() - start) / 1_000_000);
        return result;
    }

    // ------------------------------------------------------------------ 规则求值

    /** 指定级序的理论毫米值（未舍入） */
    private BigDecimal rawValue(NormRule rule, BigDecimal baseRounded, int level) {
        if (level == 0) {
            return baseRounded;
        }
        BigDecimal deltaMm;
        if (GradeDict.RULE_RATIO.equals(rule.ruleType())) {
            BigDecimal ratio = rule.ratioPercent().divide(HUNDRED, RATIO_SCALE, RoundingMode.HALF_UP);
            deltaMm = baseRounded.multiply(ratio).multiply(BigDecimal.valueOf(level));
        } else if (GradeDict.RULE_SEGMENT.equals(rule.ruleType())) {
            deltaMm = segmentDelta(rule.segments(), level);
        } else {
            // FIXED（含无规则时的固定 0）
            deltaMm = rule.fixedDeltaMm().multiply(BigDecimal.valueOf(level));
        }
        return baseRounded.add(deltaMm);
    }

    /**
     * 分段累计：从基准级序 0 走到 level，逐级累加每一步【起点级序】所在码段的每级增量。
     * 即 0→1 用覆盖 0 的段，1→2 用覆盖 1 的段；降码（如 0→-1）同样取 0 级段增量的相反数。
     */
    private BigDecimal segmentDelta(List<NormSegment> segments, int level) {
        BigDecimal total = BigDecimal.ZERO;
        if (level > 0) {
            for (int stepStart = 0; stepStart <= level - 1; stepStart++) {
                total = total.add(segmentAt(segments, stepStart));
            }
        } else {
            // 降码：0→-1 用 0 级段，-1→-2 用 -1 级段，逐段取负
            for (int stepStart = 0; stepStart >= level + 1; stepStart--) {
                total = total.subtract(segmentAt(segments, stepStart));
            }
        }
        return total;
    }

    /** 级序 level 所属码段的 delta */
    private BigDecimal segmentAt(List<NormSegment> segments, int level) {
        for (NormSegment seg : segments) {
            if (level >= seg.fromLevel() && level <= seg.toLevel()) {
                return seg.deltaMm();
            }
        }
        throw new IllegalStateException("级序 " + level + " 未被任何码段覆盖");
    }

    /**
     * 按步进做 HALF_UP 舍入：step=1 → 整毫米；step=0.1 → 0.1 毫米（旧厘米兼容）。
     * 保留与步进一致的 scale（不 stripTrailingZeros），保证 valueMm/10 恒为精确整除，
     * 厘米值只是毫米的十进制移位、不需要二次舍入。
     */
    public static BigDecimal roundToStep(BigDecimal value, BigDecimal stepMm) {
        BigDecimal divided = value.divide(stepMm, 0, RoundingMode.HALF_UP);
        return divided.multiply(stepMm);
    }

    // ------------------------------------------------------------------ 异常定位

    private void collectIssues(List<GradeIssue> issues, String part, String side,
                               NormRule rule, List<GradeCell> cells) {
        String sideText = sideText(side);
        for (GradeCell cell : cells) {
            if (cell.getValueMm().compareTo(BigDecimal.ZERO) <= 0) {
                issues.add(new GradeIssue("NEGATIVE_OR_ZERO", cell.getSizeCode(), cell.getLevel(),
                        part, side, GradeDict.partName(part) + sideText + "在尺码 " + cell.getSizeCode()
                                + " 的尺寸为 " + cell.getValueMm() + "mm，出现零长度或负数，无法制版"));
            }
        }
        // 相邻倒挂与增量方向检查
        for (int i = 1; i < cells.size(); i++) {
            GradeCell prev = cells.get(i - 1);
            GradeCell cur = cells.get(i);
            int configSign = configuredStepSign(rule, cur.getLevel(), prev.getLevel());
            int finalSign = cur.getValueMm().subtract(prev.getValueMm()).signum();

            // INVERSION：相邻尺码倒挂（最终尺寸比前一码小，无论规则意图如何都提示版师核对）
            if (finalSign < 0) {
                issues.add(new GradeIssue("INVERSION", cur.getSizeCode(), cur.getLevel(),
                        part, side, GradeDict.partName(part) + sideText + "在尺码 " + cur.getSizeCode()
                                + "（" + cur.getValueMm() + "mm）小于相邻小码 " + prev.getSizeCode()
                                + "（" + prev.getValueMm() + "mm），相邻尺码倒挂"));
            }

            // DIRECTION：分段规则相邻步的增量方向发生翻转（先放后缩/先缩后放）。
            // 当前步（prev→cur）的起点段符号与前一步不同即定位
            if (GradeDict.RULE_SEGMENT.equals(rule.ruleType())) {
                // 升码当前步起点 = prevLevel；前一步起点 = prevLevel-1。
                // 降码当前步起点 = prevLevel（降码也是从 prev 出发，取 prev 段的相反数）；前一步起点 = prevLevel+1。
                int priorStart = cur.getLevel() > 0 ? prev.getLevel() - 1 : prev.getLevel() + 1;
                int priorStepSign = configuredStepSign(rule, prev.getLevel(), priorStart);
                if (configSign != 0 && configSign != priorStepSign) {
                    issues.add(new GradeIssue("DIRECTION", cur.getSizeCode(), cur.getLevel(),
                            part, side, GradeDict.partName(part) + sideText + "从尺码 " + prev.getSizeCode()
                                    + " 到 " + cur.getSizeCode() + " 这一步的增量方向由上一码段的"
                                    + (priorStepSign > 0 ? "放大" : "缩小") + "变为"
                                    + (configSign > 0 ? "放大" : "缩小")
                                    + "，相邻码段增量方向不一致，请确认分段符号"));
                }
            }
        }
    }

    /**
     * 相邻一步（prevLevel → curLevel，尺码升序）的配置方向符号。
     * FIXED：固定增量符号；RATIO：比例符号；
     * SEGMENT：与引擎分段累计口径一致，取该步【起点级序 prevLevel】所在段的 delta 符号
     * （0→1 用 0 级段；1→2 用 1 级段）。段间符号矛盾时某一步的最终走向会与此相反。
     */
    private int configuredStepSign(NormRule rule, int curLevel, int prevLevel) {
        if (GradeDict.RULE_RATIO.equals(rule.ruleType())) {
            return rule.ratioPercent().compareTo(BigDecimal.ZERO);
        }
        if (GradeDict.RULE_SEGMENT.equals(rule.ruleType())) {
            for (NormSegment seg : rule.segments()) {
                if (prevLevel >= seg.fromLevel() && prevLevel <= seg.toLevel()) {
                    return seg.deltaMm().compareTo(BigDecimal.ZERO);
                }
            }
            return 0;
        }
        BigDecimal delta = rule.fixedDeltaMm() == null ? BigDecimal.ZERO : rule.fixedDeltaMm();
        return delta.compareTo(BigDecimal.ZERO);
    }

    /**
     * 总肩宽（左半肩 + 右半肩）÷ 胸围 明显异常。
     * 左右半肩宽分别放码、互不合并，但比例是整体版型关系，故先合计再判断，
     * 定位仍落到 shoulder 部位与具体尺码。
     */
    private void collectShoulderRatioIssues(List<GradeIssue> issues, CanonicalScheme scheme,
                                            List<GradePartResult> partResults) {
        GradePartResult bust = findPart(partResults, "bust", GradeDict.SIDE_CENTER);
        GradePartResult shoulderL = findPart(partResults, "shoulder", GradeDict.SIDE_LEFT);
        GradePartResult shoulderR = findPart(partResults, "shoulder", GradeDict.SIDE_RIGHT);
        if (bust == null || shoulderL == null || shoulderR == null) {
            return;
        }
        for (int i = 0; i < scheme.getSizes().size(); i++) {
            BigDecimal shoulderMm = shoulderL.getCells().get(i).getValueMm()
                    .add(shoulderR.getCells().get(i).getValueMm());
            BigDecimal bustMm = bust.getCells().get(i).getValueMm();
            if (bustMm.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal ratio = shoulderMm.divide(bustMm, 4, RoundingMode.HALF_UP);
            if (ratio.compareTo(GradeDict.SHOULDER_RATIO_MIN) < 0
                    || ratio.compareTo(GradeDict.SHOULDER_RATIO_MAX) > 0) {
                issues.add(new GradeIssue("SHOULDER_RATIO", scheme.getSizes().get(i),
                        scheme.getLevels().get(i), "shoulder", GradeDict.SIDE_CENTER,
                        "尺码 " + scheme.getSizes().get(i) + " 的总肩宽（左 "
                                + shoulderL.getCells().get(i).getValueMm() + "mm + 右 "
                                + shoulderR.getCells().get(i).getValueMm() + "mm = " + shoulderMm
                                + "mm）与胸围（" + bustMm + "mm）比例为 "
                                + ratio.stripTrailingZeros().toPlainString()
                                + "，超出合理带 " + GradeDict.SHOULDER_RATIO_MIN.toPlainString() + "~"
                                + GradeDict.SHOULDER_RATIO_MAX.toPlainString() + "，肩胸比例明显异常"));
            }
        }
    }

    private GradePartResult findPart(List<GradePartResult> parts, String part, String side) {
        for (GradePartResult p : parts) {
            if (p.getPart().equals(part) && p.getSide().equals(side)) {
                return p;
            }
        }
        return null;
    }

    private List<String> sidesOf(String part) {
        return GradeDict.partSymmetric(part)
                ? List.of(GradeDict.SIDE_LEFT, GradeDict.SIDE_RIGHT)
                : List.of(GradeDict.SIDE_CENTER);
    }

    private String sideText(String side) {
        return GradeDict.SIDE_CENTER.equals(side) ? "" : "（" + GradeDict.sideName(side) + "）";
    }

    // ------------------------------------------------------------------ 公式呈现与签名

    /** 中文可读公式（长度用厘米两位小数展示），页面展示与两方案比较都用它 */
    public String renderFormula(NormRule rule, BigDecimal baseRoundedMm) {
        String base = mmToCmText(baseRoundedMm);
        if (GradeDict.RULE_RATIO.equals(rule.ruleType())) {
            return "最终尺寸 = " + base + "cm × (1 + 级序 × "
                    + rule.ratioPercent().stripTrailingZeros().toPlainString() + "%)";
        }
        if (GradeDict.RULE_SEGMENT.equals(rule.ruleType())) {
            StringBuilder sb = new StringBuilder("分段累计（相对基准级序 0）：");
            for (int i = 0; i < rule.segments().size(); i++) {
                NormSegment seg = rule.segments().get(i);
                if (i > 0) {
                    sb.append("；");
                }
                sb.append("级序 ").append(levelText(seg.fromLevel())).append("~").append(levelText(seg.toLevel()))
                        .append("：每升一级 ").append(signedCm(seg.deltaMm()));
            }
            return sb.toString();
        }
        BigDecimal delta = rule.fixedDeltaMm() == null ? BigDecimal.ZERO : rule.fixedDeltaMm();
        if (delta.compareTo(BigDecimal.ZERO) == 0) {
            return "最终尺寸 = 基准 " + base + "cm（不放码）";
        }
        return "最终尺寸 = " + base + "cm + 级序 × " + signedCm(delta);
    }

    private String levelText(int level) {
        return level == 0 ? "0" : (level > 0 ? "+" + level : String.valueOf(level));
    }

    private String mmToCmText(BigDecimal mm) {
        return mm.divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private String signedCm(BigDecimal mm) {
        BigDecimal cm = mm.divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP).stripTrailingZeros();
        String sign = cm.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        return sign + cm.toPlainString() + "cm";
    }
}
