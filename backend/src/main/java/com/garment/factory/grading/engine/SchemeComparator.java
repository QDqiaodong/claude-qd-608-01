package com.garment.factory.grading.engine;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 两套放码方案逐部位比较。
 *
 * <p>差异三分类（可并存，优先级 FORMULA &gt; FINAL &gt; ROUNDING）：
 * <ul>
 *   <li>{@code FORMULA} 公式差异：规则指纹不同（固定增量/比例/分段参数不同）；</li>
 *   <li>{@code FINAL} 最终尺寸差异：最终尺寸数值不同；</li>
 *   <li>{@code ROUNDING} 舍入差异：公式相同、未舍入值相同，仅因舍入口径（量子）不同导致最终值不同；</li>
 * </ul>
 * 槽位或尺码只在一方存在时记 ONLY_A / ONLY_B。
 */
public final class SchemeComparator {

    public enum DiffKind {
        FORMULA, ROUNDING, FINAL, ONLY_A, ONLY_B, NONE
    }

    /**
     * 一个槽位在一个共有尺码上的比较明细。
     */
    public record CellDiff(
            String slotKey,
            String slotNameA,
            String slotNameB,
            int grade,
            String sizeName,
            DiffKind kind,
            BigDecimal rawA,
            BigDecimal rawB,
            BigDecimal finalA,
            BigDecimal finalB,
            BigDecimal finalDelta,
            String detail) {
    }

    /**
     * 槽位级汇总。
     */
    public record SlotDiff(
            String slotKey,
            String nameA,
            String nameB,
            boolean onlyInA,
            boolean onlyInB,
            boolean formulaDifferent,
            boolean roundingDifferent,
            boolean finalDifferent,
            List<CellDiff> cells,
            String summary) {
    }

    public record CompareResult(
            List<SlotDiff> slots,
            Map<String, Integer> counts,
            int commonSizeCount) {
    }

    private SchemeComparator() {
    }

    public static CompareResult compare(GradingResultSet a, GradingResultSet b) {
        Map<String, GradingPart> partsA = partMap(a);
        Map<String, GradingPart> partsB = partMap(b);
        Map<Integer, String> sizeNamesA = sizeNames(a);
        Map<Integer, String> sizeNamesB = sizeNames(b);

        List<String> slotKeys = new ArrayList<>();
        partsA.keySet().forEach(k -> {
            if (!slotKeys.contains(k)) {
                slotKeys.add(k);
            }
        });
        partsB.keySet().forEach(k -> {
            if (!slotKeys.contains(k)) {
                slotKeys.add(k);
            }
        });

        List<SlotDiff> slotDiffs = new ArrayList<>();
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("FORMULA", 0);
        counts.put("ROUNDING", 0);
        counts.put("FINAL", 0);
        counts.put("ONLY_A", 0);
        counts.put("ONLY_B", 0);

        int[] commonSizes = {0};

        for (String key : slotKeys) {
            GradingPart pa = partsA.get(key);
            GradingPart pb = partsB.get(key);
            if (pa == null) {
                slotDiffs.add(new SlotDiff(key, null, pb.displayName(), false, true,
                        false, false, false, List.of(), "仅方案 B 存在该部位"));
                counts.merge("ONLY_B", 1, Integer::sum);
                continue;
            }
            if (pb == null) {
                slotDiffs.add(new SlotDiff(key, pa.displayName(), null, true, false,
                        false, false, false, List.of(), "仅方案 A 存在该部位"));
                counts.merge("ONLY_A", 1, Integer::sum);
                continue;
            }

            String sigA = a.formulaSignatures().getOrDefault(key, "");
            String sigB = b.formulaSignatures().getOrDefault(key, "");
            boolean formulaDifferent = !sigA.equals(sigB);

            List<CellDiff> cellDiffs = new ArrayList<>();
            boolean roundingDifferent = false;
            boolean finalDifferent = false;

            for (GradingSpec.SizeGrade sa : a.sizes()) {
                if (b.cell(key, sa.grade()) == null) {
                    cellDiffs.add(new CellDiff(key, pa.displayName(), pb.displayName(), sa.grade(),
                            sa.name(), DiffKind.ONLY_A, null, null, null, null, null,
                            "尺码「" + sa.name() + "」仅在方案 A 中"));
                    continue;
                }
                String sizeNameB = sizeNamesB.getOrDefault(sa.grade(), sa.name());
                commonSizes[0]++;
                GradingCell ca = a.cell(key, sa.grade());
                GradingCell cb = b.cell(key, sa.grade());

                boolean rawEqual = ca.rawMicrons().compareTo(cb.rawMicrons()) == 0;
                boolean finalEqual = ca.finalMicrons().compareTo(cb.finalMicrons()) == 0;

                DiffKind kind = DiffKind.NONE;
                String detail = null;
                BigDecimal finalDelta = BigDecimal.ZERO;

                if (!finalEqual) {
                    finalDelta = cb.finalMicrons().subtract(ca.finalMicrons());
                    finalDifferent = true;
                    if (formulaDifferent) {
                        kind = DiffKind.FORMULA;
                        detail = "公式不同（A: " + sigA + "；B: " + sigB
                                + "），最终尺寸相差 " + signedCm(finalDelta);
                    } else if (rawEqual) {
                        kind = DiffKind.ROUNDING;
                        roundingDifferent = true;
                        detail = "公式与未舍入尺寸相同，仅舍入口径不同（量子 "
                                + quantumText(a.quantumMicrons()) + " vs "
                                + quantumText(b.quantumMicrons()) + "），最终相差 "
                                + signedCm(finalDelta);
                    } else {
                        kind = DiffKind.FINAL;
                        detail = "公式相同但基准/参数折算的未舍入尺寸不同，最终相差 "
                                + signedCm(finalDelta);
                    }
                } else if (formulaDifferent) {
                    // 公式不同但此尺码最终恰好相等：仍按公式差异标注（整槽位维度）
                    kind = DiffKind.NONE;
                    detail = "本尺码最终值相同，但公式不同（A: " + sigA + "；B: " + sigB + "）";
                }

                cellDiffs.add(new CellDiff(key, pa.displayName(), pb.displayName(), sa.grade(),
                        sizeNameB, kind, ca.rawMicrons(), cb.rawMicrons(),
                        ca.finalMicrons(), cb.finalMicrons(),
                        finalEqual ? BigDecimal.ZERO : finalDelta, detail));
            }

            for (GradingSpec.SizeGrade sb : b.sizes()) {
                if (a.cell(key, sb.grade()) == null) {
                    cellDiffs.add(new CellDiff(key, pa.displayName(), pb.displayName(), sb.grade(),
                            sb.name(), DiffKind.ONLY_B, null, null, null, null, null,
                            "尺码「" + sb.name() + "」仅在方案 B 中"));
                }
            }

            // 逐格统计差异类别（同一槽位可同时贡献公式差 + 舍入差）
            for (CellDiff cd : cellDiffs) {
                switch (cd.kind()) {
                    case FORMULA -> counts.merge("FORMULA", 1, Integer::sum);
                    case ROUNDING -> counts.merge("ROUNDING", 1, Integer::sum);
                    case FINAL -> counts.merge("FINAL", 1, Integer::sum);
                    case ONLY_A -> counts.merge("ONLY_A", 1, Integer::sum);
                    case ONLY_B -> counts.merge("ONLY_B", 1, Integer::sum);
                    default -> {
                    }
                }
            }

            List<String> tags = new ArrayList<>();
            if (formulaDifferent) {
                tags.add("公式差异");
            }
            if (roundingDifferent) {
                tags.add("舍入差异");
            }
            if (finalDifferent) {
                tags.add("最终尺寸差异");
            }
            String summary = tags.isEmpty() ? "两方案该部位一致" : String.join("、", tags);

            slotDiffs.add(new SlotDiff(key, pa.displayName(), pb.displayName(), false, false,
                    formulaDifferent, roundingDifferent, finalDifferent, List.copyOf(cellDiffs), summary));
        }

        return new CompareResult(List.copyOf(slotDiffs), counts, commonSizes[0]);
    }

    private static Map<String, GradingPart> partMap(GradingResultSet rs) {
        Map<String, GradingPart> map = new LinkedHashMap<>();
        rs.parts().forEach(p -> map.put(p.slotKey(), p));
        return map;
    }

    private static Map<Integer, String> sizeNames(GradingResultSet rs) {
        Map<Integer, String> map = new LinkedHashMap<>();
        rs.sizes().forEach(s -> map.put(s.grade(), s.name()));
        return map;
    }

    private static String signedCm(BigDecimal microns) {
        String v = microns.abs().divide(LengthUnit.CM.micronFactor())
                .stripTrailingZeros().toPlainString() + "cm";
        return microns.signum() < 0 ? "−" + v : (microns.signum() > 0 ? "+" + v : "0cm");
    }

    private static String quantumText(BigDecimal quantum) {
        return quantum.divide(LengthUnit.CM.micronFactor()).stripTrailingZeros().toPlainString() + "cm";
    }
}
