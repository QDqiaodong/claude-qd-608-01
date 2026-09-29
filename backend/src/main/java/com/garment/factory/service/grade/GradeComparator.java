package com.garment.factory.service.grade;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.garment.factory.domain.GradeDict;
import com.garment.factory.dto.GradeCell;
import com.garment.factory.dto.GradeCompareCell;
import com.garment.factory.dto.GradeCompareResult;
import com.garment.factory.dto.GradeCompareRow;
import com.garment.factory.dto.GradePartResult;
import com.garment.factory.dto.GradeResult;

/**
 * 两套方案逐部位比较。
 *
 * 差异口径（同一尺码、同一部位、同一侧别为一格）：
 *   FORMULA  公式不同：规则类型/参数/基准不同（以规范化签名判定）
 *   ROUNDING 公式相同，但理论值(rawMm)相同、最终值不同 —— 差异纯粹来自统一舍入
 *   VALUE    最终尺寸不同（FORMULA/ROUNDING 都会伴随 VALUE；最终尺寸相同则不算 VALUE）
 *   MISSING  该部位侧别只存在于其中一套方案
 */
@Component
public class GradeComparator {

    public GradeCompareResult compare(GradeResult a, GradeResult b,
                                      CanonicalScheme ca, CanonicalScheme cb,
                                      Long idA, String nameA, Long idB, String nameB) {
        GradeCompareResult result = new GradeCompareResult();
        result.setSchemeIdA(idA);
        result.setSchemeNameA(nameA);
        result.setSchemeIdB(idB);
        result.setSchemeNameB(nameB);

        List<String> commonSizes = new ArrayList<>(a.getSizes());
        commonSizes.retainAll(b.getSizes());
        result.setCommonSizes(commonSizes);

        Map<PartSide, GradePartResult> mapA = index(a);
        Map<PartSide, GradePartResult> mapB = index(b);
        Map<PartSide, String> sigA = signatures(ca);
        Map<PartSide, String> sigB = signatures(cb);

        List<PartSide> keys = orderedKeys(mapA, mapB);
        List<GradeCompareRow> rows = new ArrayList<>();
        int formulaCells = 0;
        int roundingCells = 0;
        int valueCells = 0;
        int identical = 0;
        int total = 0;

        for (PartSide key : keys) {
            GradePartResult pa = mapA.get(key);
            GradePartResult pb = mapB.get(key);
            GradeCompareRow row = new GradeCompareRow();
            row.setPart(key.part());
            row.setSide(key.side());
            row.setRuleTypeA(pa == null ? null : pa.getRuleType());
            row.setRuleTypeB(pb == null ? null : pb.getRuleType());
            row.setFormulaA(pa == null ? null : pa.getFormula());
            row.setFormulaB(pb == null ? null : pb.getFormula());

            boolean onlyOne = pa == null || pb == null;
            String sa = sigA.get(key);
            String sb = sigB.get(key);
            boolean formulaSame = !onlyOne && sa != null && sa.equals(sb);
            row.setFormulaSame(formulaSame);

            List<GradeCompareCell> cells = new ArrayList<>();
            for (String size : commonSizes) {
                GradeCell caCell = pa == null ? null : findCell(pa, size);
                GradeCell cbCell = pb == null ? null : findCell(pb, size);
                GradeCompareCell cell = new GradeCompareCell();
                cell.setSizeCode(size);
                cell.setLevel(caCell != null ? caCell.getLevel() : cbCell.getLevel());
                cell.setValueMmA(caCell == null ? null : caCell.getValueMm());
                cell.setValueMmB(cbCell == null ? null : cbCell.getValueMm());

                List<String> tags = new ArrayList<>();
                if (caCell == null || cbCell == null) {
                    tags.add("MISSING");
                } else {
                    BigDecimal diff = caCell.getValueMm().subtract(cbCell.getValueMm());
                    cell.setDiffMm(diff);
                    boolean valueDifferent = diff.compareTo(BigDecimal.ZERO) != 0;
                    if (!formulaSame) {
                        tags.add("FORMULA");
                    }
                    // 公式等价（同基准、同规则参数）时，最终值的任何差异都只可能来自
                    // 两方案不同的统一舍入步进（含基准被步进带动、理论值舍入落点不同），
                    // 归为舍入差异；FORMULA 与 ROUNDING 互斥。
                    if (formulaSame && valueDifferent) {
                        tags.add("ROUNDING");
                    }
                    if (valueDifferent) {
                        tags.add("VALUE");
                    }
                    cell.setFormulaSame(formulaSame);
                    if (!valueDifferent && formulaSame) {
                        identical++;
                    }
                }
                cell.setTags(tags);
                cells.add(cell);
                total++;
                if (tags.contains("FORMULA") || tags.contains("MISSING")) {
                    formulaCells++;
                    row.setFormulaDiffCount(row.getFormulaDiffCount() + 1);
                }
                if (tags.contains("ROUNDING")) {
                    roundingCells++;
                    row.setRoundingOnlyCount(row.getRoundingOnlyCount() + 1);
                }
                if (tags.contains("VALUE")) {
                    valueCells++;
                    row.setValueDiffCount(row.getValueDiffCount() + 1);
                }
            }
            row.setCells(cells);
            rows.add(row);
        }

        result.setRows(rows);
        result.setFormulaDiffCells(formulaCells);
        result.setRoundingOnlyCells(roundingCells);
        result.setValueDiffCells(valueCells);
        result.setIdenticalCells(identical);
        result.setTotalCells(total);
        return result;
    }

    private Map<PartSide, GradePartResult> index(GradeResult result) {
        Map<PartSide, GradePartResult> map = new LinkedHashMap<>();
        for (GradePartResult p : result.getParts()) {
            map.put(new PartSide(p.getPart(), p.getSide()), p);
        }
        return map;
    }

    /**
     * 规则签名只由【原始基准（未经舍入步进处理）】与规则参数构成：
     * 公式是否等价不应受 roundStepMm 影响，否则仅舍入差异会被误判成公式差异。
     */
    private Map<PartSide, String> signatures(CanonicalScheme scheme) {
        Map<PartSide, String> map = new LinkedHashMap<>();
        for (String part : GradeDict.partKeys()) {
            for (String side : (GradeDict.partSymmetric(part)
                    ? List.of(GradeDict.SIDE_LEFT, GradeDict.SIDE_RIGHT)
                    : List.of(GradeDict.SIDE_CENTER))) {
                PartSide key = new PartSide(part, side);
                NormRule rule = scheme.getRules().getOrDefault(key, NormRule.constant(part, side));
                BigDecimal rawBase = scheme.getBases().get(key);
                map.put(key, rule.type() + "|" + ruleSignatureBody(rule)
                        + "|base=" + (rawBase == null ? "" : rawBase.stripTrailingZeros().toPlainString()));
            }
        }
        return map;
    }

    private String ruleSignatureBody(NormRule rule) {
        StringBuilder sb = new StringBuilder();
        if (GradeDict.RULE_FIXED.equals(rule.ruleType())) {
            sb.append("d=").append(rule.fixedDeltaMm().stripTrailingZeros().toPlainString());
        } else if (GradeDict.RULE_RATIO.equals(rule.ruleType())) {
            sb.append("r=").append(rule.ratioPercent().stripTrailingZeros().toPlainString());
        } else {
            for (NormSegment seg : rule.segments()) {
                sb.append("[").append(seg.fromLevel()).append(',').append(seg.toLevel()).append("]=")
                        .append(seg.deltaMm().stripTrailingZeros().toPlainString()).append(';');
            }
        }
        return sb.toString();
    }

    private List<PartSide> orderedKeys(Map<PartSide, GradePartResult> a, Map<PartSide, GradePartResult> b) {
        List<PartSide> keys = new ArrayList<>();
        for (String part : GradeDict.partKeys()) {
            List<String> sides = GradeDict.partSymmetric(part)
                    ? List.of(GradeDict.SIDE_LEFT, GradeDict.SIDE_RIGHT)
                    : List.of(GradeDict.SIDE_CENTER);
            for (String side : sides) {
                PartSide key = new PartSide(part, side);
                if (a.containsKey(key) || b.containsKey(key)) {
                    keys.add(key);
                }
            }
        }
        return keys;
    }

    private GradeCell findCell(GradePartResult part, String size) {
        for (GradeCell cell : part.getCells()) {
            if (cell.getSizeCode().equals(size)) {
                return cell;
            }
        }
        return null;
    }
}
