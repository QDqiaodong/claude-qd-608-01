package com.garment.factory.domain;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 批量放码词典与规则参数。
 *
 * 精度口径（服务端统一保证）：
 *   1. 内部一切计算以【毫米】为规范单位，使用 BigDecimal，避免 double 累计误差；
 *   2. 每个部位原始值算完后只按方案级「舍入步进（毫米）」做一次四舍五入（HALF_UP）；
 *   3. 厘米值一律由毫米值除以 10 派生，前端切单位只换显示，绝不重新舍入，因此来回切换不漂移；
 *   4. 旧数据只有厘米两位小数：按兼容规则读入（inputUnit=CM、roundStepMm=0.1），重算结果与旧值一致。
 */
public final class GradeDict {

    private GradeDict() {
    }

    // ---------------- 输入单位 ----------------
    public static final String UNIT_CM = "CM";
    public static final String UNIT_MM = "MM";

    private static final Map<String, String> UNIT = new LinkedHashMap<>();
    static {
        UNIT.put(UNIT_CM, "厘米");
        UNIT.put(UNIT_MM, "毫米");
    }

    // ---------------- 放码规则类型 ----------------
    /** 固定增量：每升一级累加固定毫米数（降码为其相反数累加） */
    public static final String RULE_FIXED = "FIXED";
    /** 按比例：每级 = 基准尺寸 × 比例（%），按级次线性变化 */
    public static final String RULE_RATIO = "RATIO";
    /** 分段：按尺码级序落在不同码段，各段使用各自的每级增量并逐段累计 */
    public static final String RULE_SEGMENT = "SEGMENT";

    private static final Map<String, String> RULE_TYPE = new LinkedHashMap<>();
    static {
        RULE_TYPE.put(RULE_FIXED, "固定增量");
        RULE_TYPE.put(RULE_RATIO, "按比例变化");
        RULE_TYPE.put(RULE_SEGMENT, "分段增量");
    }

    // ---------------- 部位侧别 ----------------
    public static final String SIDE_CENTER = "CENTER";
    public static final String SIDE_LEFT = "LEFT";
    public static final String SIDE_RIGHT = "RIGHT";

    private static final Map<String, String> SIDE = new LinkedHashMap<>();
    static {
        SIDE.put(SIDE_CENTER, "不分左右");
        SIDE.put(SIDE_LEFT, "左侧");
        SIDE.put(SIDE_RIGHT, "右侧");
    }

    // ---------------- 可放码部位 ----------------
    /** key 顺序即页面与结果表的展示顺序 */
    private static final List<String> PART_KEYS = List.of(
            "bust", "waist", "hip", "shoulder", "bodyLength", "sleeveLength");

    private static final Map<String, String> PART_NAME = new LinkedHashMap<>();
    private static final Map<String, String> PART_SYMMETRIC = new LinkedHashMap<>();
    static {
        PART_NAME.put("bust", "胸围");
        PART_NAME.put("waist", "腰围");
        PART_NAME.put("hip", "臀围");
        PART_NAME.put("shoulder", "肩宽");
        PART_NAME.put("bodyLength", "衣长");
        PART_NAME.put("sleeveLength", "袖长");
        // 肩宽、袖长支持左右不对称：版师可对 LEFT/RIGHT 分别建规则，系统不得自动合并
        PART_SYMMETRIC.put("bust", SIDE_CENTER);
        PART_SYMMETRIC.put("waist", SIDE_CENTER);
        PART_SYMMETRIC.put("hip", SIDE_CENTER);
        PART_SYMMETRIC.put("shoulder", SIDE_LEFT);
        PART_SYMMETRIC.put("bodyLength", SIDE_CENTER);
        PART_SYMMETRIC.put("sleeveLength", SIDE_LEFT);
    }

    // ---------------- 校验阈值 ----------------
    /** 肩宽/胸围比例合理带（半肩宽 ÷ 胸围）：女装肩宽通常为胸围 0.40~0.58 倍 */
    public static final BigDecimal SHOULDER_RATIO_MIN = new BigDecimal("0.34");
    public static final BigDecimal SHOULDER_RATIO_MAX = new BigDecimal("0.62");
    /** 超过该倍数视为「明显异常」（宽到离谱），0.34 以下为过窄 */

    /** 默认舍入步进：1 毫米 */
    public static final BigDecimal DEFAULT_ROUND_STEP_MM = BigDecimal.ONE;
    /** 旧厘米数据兼容舍入步进：0.1 毫米（=0.01cm），保证两位小数厘米值精确还原 */
    public static final BigDecimal LEGACY_ROUND_STEP_MM = new BigDecimal("0.1");

    public static List<String> partKeys() {
        return PART_KEYS;
    }

    public static Map<String, String> parts() {
        return PART_NAME;
    }

    public static String partName(String part) {
        return part == null ? "" : PART_NAME.getOrDefault(part, part);
    }

    /** 该部位是否支持左右不对称（肩宽、袖长） */
    public static boolean partSymmetric(String part) {
        return SIDE_LEFT.equals(PART_SYMMETRIC.get(part));
    }

    public static boolean isValidPart(String part) {
        return part != null && PART_NAME.containsKey(part);
    }

    public static boolean isValidUnit(String unit) {
        return unit != null && UNIT.containsKey(unit);
    }

    public static String unitName(String unit) {
        return unit == null ? "" : UNIT.getOrDefault(unit, unit);
    }

    public static Map<String, String> units() {
        return UNIT;
    }

    public static boolean isValidRuleType(String type) {
        return type != null && RULE_TYPE.containsKey(type);
    }

    public static String ruleTypeName(String type) {
        return type == null ? "" : RULE_TYPE.getOrDefault(type, type);
    }

    public static Map<String, String> ruleTypes() {
        return RULE_TYPE;
    }

    public static boolean isValidSide(String side) {
        return side != null && SIDE.containsKey(side);
    }

    public static String sideName(String side) {
        return side == null ? "" : SIDE.getOrDefault(side, side);
    }

    public static Map<String, String> sides() {
        return SIDE;
    }
}
