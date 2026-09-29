package com.garment.factory.service.grade;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.garment.factory.common.BizException;
import com.garment.factory.domain.GradeDict;
import com.garment.factory.dto.GradeBaseReq;
import com.garment.factory.dto.GradeRuleReq;
import com.garment.factory.dto.GradeSchemeReq;
import com.garment.factory.dto.GradeSegmentReq;

/**
 * 请求 → 规范化方案：
 *   - 把厘米/毫米录入值统一折成毫米；
 *   - 校验尺码序列、基准码、部位/侧别、三种规则的参数与分段覆盖；
 *   - 任一硬性问题直接抛 BizException（公式无法计算），由调用方保证旧结果原样保留。
 */
@Component
public class GradeNormalizer {

    /** 厘米 → 毫米 */
    public static BigDecimal cmToMm(BigDecimal cm) {
        return cm.multiply(BigDecimal.TEN);
    }

    /** 毫米 → 厘米（仅用于回显，保留原值精度） */
    public static BigDecimal mmToCm(BigDecimal mm) {
        return mm.divide(BigDecimal.TEN);
    }

    public CanonicalScheme normalize(GradeSchemeReq req) {
        if (req == null) {
            throw new BizException("放码参数为空");
        }
        String unit = req.getInputUnit() == null ? GradeDict.UNIT_CM : req.getInputUnit().trim().toUpperCase();
        if (!GradeDict.isValidUnit(unit)) {
            throw new BizException("录入单位不合法，只能是 CM(厘米) 或 MM(毫米)");
        }
        BigDecimal factor = GradeDict.UNIT_MM.equals(unit) ? BigDecimal.ONE : BigDecimal.TEN;

        BigDecimal roundStep = req.getRoundStepMm() == null ? GradeDict.DEFAULT_ROUND_STEP_MM : req.getRoundStepMm();
        if (roundStep.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("舍入步进必须大于 0");
        }

        List<String> sizes = normalizeSizes(req.getSizes());
        String baseCode = req.getBaseSizeCode() == null ? "" : req.getBaseSizeCode().trim();
        int baseIndex = sizes.indexOf(baseCode);
        if (baseIndex < 0) {
            throw new BizException("基准尺码「" + baseCode + "」不在目标尺码序列中");
        }
        List<Integer> levels = new ArrayList<>();
        for (int i = 0; i < sizes.size(); i++) {
            levels.add(i - baseIndex);
        }

        Map<PartSide, BigDecimal> bases = new LinkedHashMap<>();
        if (req.getBases() != null) {
            for (GradeBaseReq base : req.getBases()) {
                PartSide key = validPartSide(base.getPart(), base.getSide(), "基准尺寸");
                if (base.getValue() == null) {
                    throw new BizException(GradeDict.partName(key.part()) + displaySide(key.side()) + "的基准尺寸为空");
                }
                BigDecimal valueMm = base.getValue().multiply(factor);
                // 基准允许很小（小到整毫米舍入后为 0，由结果异常定位提示），只拒绝非正数输入
                if (valueMm.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BizException(GradeDict.partName(key.part()) + displaySide(key.side()) + "的基准尺寸必须大于 0");
                }
                // 保留录入精度（CM 两位小数 ×10 后 scale=1，MM 为整数），不 stripTrailingZeros，
                // 这样基准毫米值除以 10 恒为精确整除，厘米显示无需二次舍入
                bases.put(key, valueMm);
            }
        }
        for (String part : GradeDict.partKeys()) {
            if (GradeDict.partSymmetric(part)) {
                if (!bases.containsKey(new PartSide(part, GradeDict.SIDE_LEFT))
                        || !bases.containsKey(new PartSide(part, GradeDict.SIDE_RIGHT))) {
                    throw new BizException(GradeDict.partName(part) + "是左右不对称部位，必须分别录入左侧、右侧基准尺寸");
                }
            } else if (!bases.containsKey(new PartSide(part, GradeDict.SIDE_CENTER))) {
                throw new BizException("缺少部位基准尺寸：" + GradeDict.partName(part));
            }
        }

        Map<PartSide, NormRule> rules = new LinkedHashMap<>();
        if (req.getRules() != null) {
            for (GradeRuleReq rule : req.getRules()) {
                PartSide key = validPartSide(rule.getPart(), rule.getSide(), "放码规则");
                if (rules.containsKey(key)) {
                    throw new BizException(GradeDict.partName(key.part()) + displaySide(key.side()) + "存在重复规则");
                }
                rules.put(key, normalizeRule(rule, factor, levels));
            }
        }

        CanonicalScheme scheme = new CanonicalScheme();
        scheme.setInputUnit(unit);
        scheme.setRoundStepMm(roundStep);
        scheme.setSizes(sizes);
        scheme.setLevels(levels);
        scheme.setBaseSizeCode(baseCode);
        scheme.setBaseIndex(baseIndex);
        scheme.setBases(bases);
        scheme.setRules(rules);
        scheme.setLegacy(Boolean.TRUE.equals(req.getLegacyFlag()));
        return scheme;
    }

    private NormRule normalizeRule(GradeRuleReq rule, BigDecimal factor, List<Integer> levels) {
        String type = rule.getRuleType() == null ? "" : rule.getRuleType().trim().toUpperCase();
        PartSide key = validPartSide(rule.getPart(), rule.getSide(), "放码规则");
        String where = GradeDict.partName(key.part()) + displaySide(key.side());

        if (type.isBlank()) {
            // 无规则：视为固定增量 0，所有尺码等于基准
            return NormRule.constant(key.part(), key.side());
        }
        if (!GradeDict.isValidRuleType(type)) {
            throw new BizException(where + "的放码规则类型不合法：" + type);
        }

        if (GradeDict.RULE_FIXED.equals(type)) {
            if (rule.getFixedDelta() == null) {
                throw new BizException(where + "的固定增量为空");
            }
            return new NormRule(key.part(), key.side(), type,
                    rule.getFixedDelta().multiply(factor).stripTrailingZeros(), null, List.of());
        }

        if (GradeDict.RULE_RATIO.equals(type)) {
            if (rule.getRatioPercent() == null) {
                throw new BizException(where + "的比例（%）为空");
            }
            return new NormRule(key.part(), key.side(), type, null, rule.getRatioPercent(), List.of());
        }

        // SEGMENT
        List<GradeSegmentReq> raw = rule.getSegments();
        if (raw == null || raw.isEmpty()) {
            throw new BizException(where + "的分段规则至少需要一个码段");
        }
        List<NormSegment> segments = new ArrayList<>();
        for (GradeSegmentReq seg : raw) {
            if (seg.getFromLevel() == null || seg.getToLevel() == null) {
                throw new BizException(where + "存在未指定级序区间的码段");
            }
            if (seg.getFromLevel() > seg.getToLevel()) {
                throw new BizException(where + "存在起止倒挂的码段：" + seg.getFromLevel() + "→" + seg.getToLevel());
            }
            if (seg.getDelta() == null) {
                throw new BizException(where + "码段[" + seg.getFromLevel() + "," + seg.getToLevel() + "]的增量为空");
            }
            segments.add(new NormSegment(seg.getFromLevel(), seg.getToLevel(),
                    seg.getDelta().multiply(factor).stripTrailingZeros()));
        }
        segments.sort(Comparator.comparingInt(NormSegment::fromLevel));
        // 段内不得重叠
        for (int i = 1; i < segments.size(); i++) {
            if (segments.get(i).fromLevel() <= segments.get(i - 1).toLevel()) {
                throw new BizException(where + "的码段存在重叠：[" + segments.get(i - 1).fromLevel() + ","
                        + segments.get(i - 1).toLevel() + "] 与 [" + segments.get(i).fromLevel() + ","
                        + segments.get(i).toLevel() + "]");
            }
            if (segments.get(i).fromLevel() != segments.get(i - 1).toLevel() + 1) {
                throw new BizException(where + "的码段之间不连续，请用增量为 0 的码段补齐空档");
            }
        }
        int minLevel = levels.get(0);
        int maxLevel = levels.get(levels.size() - 1);
        if (segments.get(0).fromLevel() != minLevel || segments.get(segments.size() - 1).toLevel() != maxLevel) {
            throw new BizException(where + "的码段必须从最小级序 " + minLevel + " 连续覆盖到最大级序 " + maxLevel);
        }
        return new NormRule(key.part(), key.side(), type, null, null, List.copyOf(segments));
    }

    private PartSide validPartSide(String part, String side, String scene) {
        if (part == null || !GradeDict.isValidPart(part.trim())) {
            throw new BizException(scene + "中存在未知部位：" + part);
        }
        String p = part.trim();
        String s = side == null || side.isBlank() ? GradeDict.SIDE_CENTER : side.trim().toUpperCase();
        if (!GradeDict.isValidSide(s)) {
            throw new BizException(GradeDict.partName(p) + "的侧别不合法：" + s);
        }
        boolean symmetric = GradeDict.partSymmetric(p);
        if (!symmetric && !GradeDict.SIDE_CENTER.equals(s)) {
            throw new BizException(GradeDict.partName(p) + "不是左右部位，只能使用「不分左右」");
        }
        if (symmetric && GradeDict.SIDE_CENTER.equals(s)) {
            throw new BizException(GradeDict.partName(p) + "是左右不对称部位，请分别选择左侧或右侧，不能合并成一条");
        }
        return new PartSide(p, s);
    }

    private List<String> normalizeSizes(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            throw new BizException("目标尺码序列为空");
        }
        List<String> sizes = new ArrayList<>();
        for (String s : raw) {
            if (s == null || s.isBlank()) {
                throw new BizException("尺码序列中存在空尺码");
            }
            String code = s.trim();
            if (sizes.contains(code)) {
                throw new BizException("尺码序列存在重复尺码：" + code);
            }
            sizes.add(code);
        }
        return sizes;
    }

    private String displaySide(String side) {
        return GradeDict.SIDE_CENTER.equals(side) ? "" : "（" + GradeDict.sideName(side) + "）";
    }
}
