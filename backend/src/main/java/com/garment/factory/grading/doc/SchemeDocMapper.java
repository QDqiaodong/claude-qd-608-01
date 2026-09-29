package com.garment.factory.grading.doc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.garment.factory.common.BizException;
import com.garment.factory.grading.engine.DecimalQuantity;
import com.garment.factory.grading.engine.Direction;
import com.garment.factory.grading.engine.GradingPart;
import com.garment.factory.grading.engine.GradingSpec;
import com.garment.factory.grading.engine.LengthUnit;
import com.garment.factory.grading.engine.RatioCheck;
import com.garment.factory.grading.engine.RuleConfig;
import com.garment.factory.grading.engine.RuleType;
import com.garment.factory.grading.engine.Segment;

/**
 * 把外部方案文档 {@link SchemeDoc} 解析成引擎输入 {@link GradingSpec}。
 *
 * <p>关键：在这里把所有 cm/mm 字符串精确折算成微米有理数量；之后引擎与存储中的数值口径完全统一，
 * 页面切换单位只重新渲染显示，不再走解析/舍入。
 */
@Component
public class SchemeDocMapper {

    public GradingSpec toSpec(SchemeDoc doc) {
        LengthUnit inputUnit = LengthUnit.of(doc.getInputUnit());

        if (doc.getRounding() == null) {
            throw new BizException("缺少统一舍入配置");
        }
        BigDecimal quantum = GradingRoundingBridge.quantum(doc.getRounding());

        List<GradingSpec.SizeGrade> sizes = new ArrayList<>();
        for (SchemeDoc.SizeDoc s : doc.getSizes()) {
            if (s.getGrade() == null) {
                throw new BizException("尺码级差不能为空");
            }
            String name = s.getName() == null || s.getName().isBlank()
                    ? "级差" + s.getGrade() : s.getName().trim();
            sizes.add(new GradingSpec.SizeGrade(s.getGrade(), name));
        }
        sizes.sort(java.util.Comparator.comparingInt(GradingSpec.SizeGrade::grade));

        List<GradingPart> parts = new ArrayList<>();
        Map<String, GradingPart> slotIndex = new LinkedHashMap<>();
        for (SchemeDoc.PartDoc p : doc.getParts()) {
            if (p.getPartId() == null || p.getPartId().isBlank()) {
                throw new BizException("存在缺少编码（partId）的部位");
            }
            String side = normalizeSide(p.getSide());
            LengthUnit partUnit = p.getUnit() == null || p.getUnit().isBlank()
                    ? inputUnit : LengthUnit.of(p.getUnit());
            BigDecimal baseMicrons = DecimalQuantity.parseLength(p.getBase(), partUnit);

            RuleConfig rule = toRule(p, inputUnit, p.getPartId() + (side == null ? "" : "#" + side));
            Direction direction = Direction.of(p.getDirection());
            GradingPart part = new GradingPart(
                    p.getPartId().trim(),
                    p.getPartName() == null || p.getPartName().isBlank() ? p.getPartId() : p.getPartName().trim(),
                    side, baseMicrons, direction, rule);
            if (slotIndex.put(part.slotKey(), part) != null) {
                throw new BizException("部位槽位重复：" + part.slotKey());
            }
            parts.add(part);
        }

        List<RatioCheck> checks = new ArrayList<>();
        for (SchemeDoc.RatioCheckDoc c : doc.getRatioChecks()) {
            checks.add(new RatioCheck(
                    c.getCheckCode() == null ? "RATIO" : c.getCheckCode(),
                    requireText(c.getNumeratorSlot(), "比例检查分子槽位"),
                    requireText(c.getDenominatorSlot(), "比例检查分母槽位"),
                    parseRatioBound(c.getMinRatio(), "比例下限"),
                    parseRatioBound(c.getMaxRatio(), "比例上限"),
                    c.getLabel() == null || c.getLabel().isBlank()
                            ? c.getNumeratorSlot() + "/" + c.getDenominatorSlot() : c.getLabel()));
        }

        return new GradingSpec(sizes, doc.getBaseGrade() == null ? 0 : doc.getBaseGrade(),
                quantum, parts, checks);
    }

    private RuleConfig toRule(SchemeDoc.PartDoc p, LengthUnit inputUnit, String slotDesc) {
        SchemeDoc.RuleDoc r = p.getRule();
        if (r == null) {
            throw new BizException("部位「" + slotDesc + "」缺少放码规则");
        }
        RuleType type = RuleType.of(r.getType());
        RuleConfig config = new RuleConfig();
        config.setType(type);
        LengthUnit ruleUnit = r.getUnit() == null || r.getUnit().isBlank()
                ? (p.getUnit() == null || p.getUnit().isBlank() ? inputUnit : LengthUnit.of(p.getUnit()))
                : LengthUnit.of(r.getUnit());
        switch (type) {
            case FIXED -> {
                if (r.getDelta() == null || r.getDelta().isBlank()) {
                    throw new BizException("部位「" + slotDesc + "」固定增量规则缺少变化量 delta");
                }
                config.setDeltaMicrons(DecimalQuantity.parseDelta(r.getDelta(), ruleUnit));
            }
            case RATIO -> {
                if (r.getRate() == null || r.getRate().isBlank()) {
                    throw new BizException("部位「" + slotDesc + "」比例规则缺少 rate");
                }
                config.setRate(DecimalQuantity.parseRate(r.getRate()));
            }
            case SEGMENT -> {
                if (r.getSegments() == null || r.getSegments().isEmpty()) {
                    throw new BizException("部位「" + slotDesc + "」分段规则没有任何区段");
                }
                List<Segment> segments = new ArrayList<>();
                for (SchemeDoc.SegmentDoc seg : r.getSegments()) {
                    if (seg.getFromGrade() == null || seg.getToGrade() == null) {
                        throw new BizException("部位「" + slotDesc + "」的分段必须给出 fromGrade/toGrade");
                    }
                    LengthUnit segUnit = seg.getUnit() == null || seg.getUnit().isBlank()
                            ? ruleUnit : LengthUnit.of(seg.getUnit());
                    if (seg.getDelta() == null || seg.getDelta().isBlank()) {
                        throw new BizException("部位「" + slotDesc + "」区段 "
                                + seg.getFromGrade() + ".." + seg.getToGrade() + " 缺少 delta");
                    }
                    segments.add(new Segment(seg.getFromGrade(), seg.getToGrade(),
                            DecimalQuantity.parseDelta(seg.getDelta(), segUnit)));
                }
                config.setSegments(segments);
            }
        }
        return config;
    }

    private String normalizeSide(String side) {
        if (side == null || side.isBlank()) {
            return null;
        }
        String s = side.trim().toUpperCase();
        return switch (s) {
            case "L", "LEFT", "左" -> "L";
            case "R", "RIGHT", "右" -> "R";
            default -> throw new BizException("侧别只能是 L/左 或 R/右：" + side);
        };
    }

    private BigDecimal parseRatioBound(String text, String label) {
        if (text == null || text.isBlank()) {
            throw new BizException(label + "不能为空");
        }
        try {
            return new BigDecimal(text.trim().replace("%", ""));
        } catch (NumberFormatException e) {
            throw new BizException(label + "不是合法数字：" + text);
        }
    }

    private String requireText(String s, String label) {
        if (s == null || s.isBlank()) {
            throw new BizException(label + "不能为空");
        }
        return s.trim();
    }
}
