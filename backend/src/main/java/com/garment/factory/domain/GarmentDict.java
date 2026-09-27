package com.garment.factory.domain;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 服装工坊词典与业务规则参数表（品类基准工价区间、量体合理区间、低库存线、定金比例、最短工期等）。
 */
public final class GarmentDict {

    private GarmentDict() {
    }

    // ---------------- 款式品类 ----------------
    private static final Map<String, String> CATEGORY = new LinkedHashMap<>();
    static {
        CATEGORY.put("SUIT", "西装");
        CATEGORY.put("OVERCOAT", "大衣");
        CATEGORY.put("SHIRT", "衬衫");
        CATEGORY.put("DRESS", "礼服");
        CATEGORY.put("PANTS", "裤装");
    }

    /** 各品类合理基础价下限 */
    private static final Map<String, BigDecimal> CATEGORY_MIN_PRICE = new LinkedHashMap<>();
    static {
        CATEGORY_MIN_PRICE.put("SUIT", BigDecimal.valueOf(2000));
        CATEGORY_MIN_PRICE.put("OVERCOAT", BigDecimal.valueOf(3000));
        CATEGORY_MIN_PRICE.put("SHIRT", BigDecimal.valueOf(600));
        CATEGORY_MIN_PRICE.put("DRESS", BigDecimal.valueOf(4000));
        CATEGORY_MIN_PRICE.put("PANTS", BigDecimal.valueOf(700));
    }

    /** 各品类标准用料（米），下单时给出推荐用量 */
    private static final Map<String, BigDecimal> CATEGORY_DEFAULT_METERS = new LinkedHashMap<>();
    static {
        CATEGORY_DEFAULT_METERS.put("SUIT", BigDecimal.valueOf(3.6));
        CATEGORY_DEFAULT_METERS.put("OVERCOAT", BigDecimal.valueOf(4.2));
        CATEGORY_DEFAULT_METERS.put("SHIRT", BigDecimal.valueOf(2.2));
        CATEGORY_DEFAULT_METERS.put("DRESS", BigDecimal.valueOf(5.0));
        CATEGORY_DEFAULT_METERS.put("PANTS", BigDecimal.valueOf(1.8));
    }

    /** 各工序默认负责人 */
    private static final Map<String, String> STEP_OWNER = new LinkedHashMap<>();
    static {
        STEP_OWNER.put("CUTTING", "陈师傅");
        STEP_OWNER.put("SEWING", "王姐");
        STEP_OWNER.put("OVERLOCK", "刘工");
        STEP_OWNER.put("PRESSING", "孙阿姨");
        STEP_OWNER.put("FITTING", "方绮");
        STEP_OWNER.put("BUTTON", "王姐");
    }

    /** 各工序标准工时（小时） */
    private static final Map<String, BigDecimal> STEP_HOURS = new LinkedHashMap<>();
    static {
        STEP_HOURS.put("CUTTING", BigDecimal.valueOf(3.5));
        STEP_HOURS.put("SEWING", BigDecimal.valueOf(8.5));
        STEP_HOURS.put("OVERLOCK", BigDecimal.valueOf(2.0));
        STEP_HOURS.put("PRESSING", BigDecimal.valueOf(1.5));
        STEP_HOURS.put("FITTING", BigDecimal.valueOf(2.0));
        STEP_HOURS.put("BUTTON", BigDecimal.valueOf(1.0));
    }

    /** 投产时按此顺序自动生成工序工单 */
    /** 工艺难度上限 */
    public static final int MAX_CRAFT_LEVEL = 5;

    private static final List<String> STEP_SEQUENCE = List.of("CUTTING", "SEWING", "OVERLOCK", "PRESSING", "FITTING", "BUTTON");

    // ---------------- 面料成分 ----------------
    private static final Map<String, String> COMPOSITION = new LinkedHashMap<>();
    static {
        COMPOSITION.put("WOOL", "羊毛");
        COMPOSITION.put("CASHMERE", "羊绒");
        COMPOSITION.put("COTTON", "棉");
        COMPOSITION.put("SILK", "真丝");
        COMPOSITION.put("BLEND", "混纺");
    }

    /** 低库存预警线（米） */
    public static final BigDecimal LOW_STOCK_LINE = BigDecimal.valueOf(50);

    /** 单次最多领用米数（超过要拆单） */
    public static final BigDecimal MAX_USAGE = BigDecimal.valueOf(20);

    /** 各部位尺寸合理区间（cm）：\<部位, {min, max}\> */
    private static final Map<String, BigDecimal[]> PART_RANGE = new LinkedHashMap<>();
    static {
        PART_RANGE.put("bust", new BigDecimal[]{BigDecimal.valueOf(60), BigDecimal.valueOf(170)});
        PART_RANGE.put("waist", new BigDecimal[]{BigDecimal.valueOf(45), BigDecimal.valueOf(160)});
        PART_RANGE.put("hip", new BigDecimal[]{BigDecimal.valueOf(60), BigDecimal.valueOf(180)});
        PART_RANGE.put("shoulder", new BigDecimal[]{BigDecimal.valueOf(25), BigDecimal.valueOf(70)});
        PART_RANGE.put("sleeveLength", new BigDecimal[]{BigDecimal.valueOf(35), BigDecimal.valueOf(90)});
        PART_RANGE.put("bodyLength", new BigDecimal[]{BigDecimal.valueOf(40), BigDecimal.valueOf(140)});
        PART_RANGE.put("pantsLength", new BigDecimal[]{BigDecimal.valueOf(50), BigDecimal.valueOf(130)});
    }

    private static final Map<String, String> PART_NAME = new LinkedHashMap<>();
    static {
        PART_NAME.put("bust", "胸围");
        PART_NAME.put("waist", "腰围");
        PART_NAME.put("hip", "臀围");
        PART_NAME.put("shoulder", "肩宽");
        PART_NAME.put("sleeveLength", "袖长");
        PART_NAME.put("bodyLength", "衣长");
        PART_NAME.put("pantsLength", "裤长");
    }

    // ---------------- 订单状态流转 ----------------
    private static final List<String> ORDER_STATUS_ORDER =
            List.of("ORDERED", "MEASURED", "CUT", "SEWING", "FITTING", "FINISHED", "DELIVERED");

    private static final Map<String, String> ORDER_STATUS = new LinkedHashMap<>();
    static {
        ORDER_STATUS.put("ORDERED", "已下单");
        ORDER_STATUS.put("MEASURED", "已量体");
        ORDER_STATUS.put("CUT", "已裁剪");
        ORDER_STATUS.put("SEWING", "缝制中");
        ORDER_STATUS.put("FITTING", "试衣");
        ORDER_STATUS.put("FINISHED", "已完成");
        ORDER_STATUS.put("DELIVERED", "已交付");
    }

    /** 最短工期（天）：交期必须晚于下单日 */
    public static final int MIN_LEAD_DAYS = 7;

    /** 定金比例 50% */
    public static final BigDecimal DEPOSIT_RATE = BigDecimal.valueOf(0.5);

    // ---------------- 工序与工单状态 ----------------
    private static final Map<String, String> STEP = new LinkedHashMap<>();
    static {
        STEP.put("CUTTING", "裁剪");
        STEP.put("SEWING", "缝制");
        STEP.put("OVERLOCK", "锁边");
        STEP.put("PRESSING", "熨烫");
        STEP.put("FITTING", "试衣");
        STEP.put("BUTTON", "钉扣");
    }

    private static final Map<String, String> WO_STATUS = new LinkedHashMap<>();
    static {
        WO_STATUS.put("PENDING", "待开工");
        WO_STATUS.put("RUNNING", "进行中");
        WO_STATUS.put("DONE", "已完成");
        WO_STATUS.put("REWORK", "返工");
    }

    /** 工单实际工时超过计划工时的倍数上限 */
    public static final BigDecimal MAX_HOURS_RATE = BigDecimal.valueOf(3);

    // ---------------- 试衣整改台 ----------------
    /** 试衣问题单状态：待处理 → 返修中 → 待复核 → 已关闭 */
    private static final List<String> ISSUE_STATUS_ORDER =
            List.of("PENDING", "REWORKING", "REVIEWING", "CLOSED");

    private static final Map<String, String> ISSUE_STATUS = new LinkedHashMap<>();
    static {
        ISSUE_STATUS.put("PENDING", "待处理");
        ISSUE_STATUS.put("REWORKING", "返修中");
        ISSUE_STATUS.put("REVIEWING", "待复核");
        ISSUE_STATUS.put("CLOSED", "已关闭");
    }

    private static final Map<String, String> ISSUE_SEVERITY = new LinkedHashMap<>();
    static {
        ISSUE_SEVERITY.put("MINOR", "普通问题");
        ISSUE_SEVERITY.put("BLOCKER", "阻断交付");
    }

    /** 问题部位（成衣视角） */
    private static final Map<String, String> BODY_PART = new LinkedHashMap<>();
    static {
        BODY_PART.put("SHOULDER", "肩部");
        BODY_PART.put("CHEST", "胸背");
        BODY_PART.put("WAIST", "腰腹");
        BODY_PART.put("HIP", "臀胯");
        BODY_PART.put("SLEEVE", "袖子");
        BODY_PART.put("COLLAR", "衣领");
        BODY_PART.put("LAPEL", "门襟驳头");
        BODY_PART.put("HEM", "下摆裤脚");
        BODY_PART.put("OTHER", "其他部位");
    }

    /** 返修轮次状态 */
    private static final Map<String, String> REWORK_STATUS = new LinkedHashMap<>();
    static {
        REWORK_STATUS.put("RUNNING", "返修中");
        REWORK_STATUS.put("DONE", "待复核");
        REWORK_STATUS.put("PASSED", "复核通过");
        REWORK_STATUS.put("REJECTED", "复核不通过");
    }

    /** 整改操作流水类型 */
    private static final Map<String, String> ISSUE_ACTION = new LinkedHashMap<>();
    static {
        ISSUE_ACTION.put("CREATE", "建立问题单");
        ISSUE_ACTION.put("START_REWORK", "发起返修");
        ISSUE_ACTION.put("FINISH_REWORK", "返修完成");
        ISSUE_ACTION.put("SUBMIT_REVIEW", "提交复核");
        ISSUE_ACTION.put("REVIEW_PASS", "复核通过");
        ISSUE_ACTION.put("REVIEW_REJECT", "复核不通过");
        ISSUE_ACTION.put("REOPEN", "重新打开");
        ISSUE_ACTION.put("RELEASE", "普通问题放行");
        ISSUE_ACTION.put("FINISH_ORDER", "订单完工");
    }

    public static boolean isValidIssueStatus(String status) {
        return status != null && ISSUE_STATUS.containsKey(status);
    }

    public static String issueStatusName(String status) {
        return status == null ? "" : ISSUE_STATUS.getOrDefault(status, status);
    }

    public static Map<String, String> issueStatuses() {
        return ISSUE_STATUS;
    }

    public static List<String> issueStatusOrder() {
        return ISSUE_STATUS_ORDER;
    }

    public static int issueStatusIndex(String status) {
        int index = ISSUE_STATUS_ORDER.indexOf(status);
        return index < 0 ? -1 : index;
    }

    public static boolean isValidSeverity(String severity) {
        return severity != null && ISSUE_SEVERITY.containsKey(severity);
    }

    public static String severityName(String severity) {
        return severity == null ? "" : ISSUE_SEVERITY.getOrDefault(severity, severity);
    }

    public static Map<String, String> severities() {
        return ISSUE_SEVERITY;
    }

    public static boolean isValidBodyPart(String part) {
        return part != null && BODY_PART.containsKey(part);
    }

    public static String bodyPartName(String part) {
        return part == null ? "" : BODY_PART.getOrDefault(part, part);
    }

    public static Map<String, String> bodyParts() {
        return BODY_PART;
    }

    public static boolean isValidReworkStatus(String status) {
        return status != null && REWORK_STATUS.containsKey(status);
    }

    public static String reworkStatusName(String status) {
        return status == null ? "" : REWORK_STATUS.getOrDefault(status, status);
    }

    public static Map<String, String> reworkStatuses() {
        return REWORK_STATUS;
    }

    public static String issueActionName(String action) {
        return action == null ? "" : ISSUE_ACTION.getOrDefault(action, action);
    }

    public static Map<String, String> issueActions() {
        return ISSUE_ACTION;
    }

    public static boolean isValidCategory(String category) {
        return category != null && CATEGORY.containsKey(category);
    }

    public static String categoryName(String category) {
        return category == null ? "" : CATEGORY.getOrDefault(category, category);
    }

    public static Map<String, String> categories() {
        return CATEGORY;
    }

    public static BigDecimal categoryMinPrice(String category) {
        return CATEGORY_MIN_PRICE.getOrDefault(category, BigDecimal.ZERO);
    }

    public static BigDecimal categoryDefaultMeters(String category) {
        return CATEGORY_DEFAULT_METERS.getOrDefault(category, BigDecimal.valueOf(3));
    }

    public static boolean isValidComposition(String composition) {
        return composition != null && COMPOSITION.containsKey(composition);
    }

    public static String compositionName(String composition) {
        return composition == null ? "" : COMPOSITION.getOrDefault(composition, composition);
    }

    public static Map<String, String> compositions() {
        return COMPOSITION;
    }

    public static String partName(String part) {
        return part == null ? "" : PART_NAME.getOrDefault(part, part);
    }

    public static BigDecimal partMin(String part) {
        BigDecimal[] range = PART_RANGE.get(part);
        return range == null ? BigDecimal.ZERO : range[0];
    }

    public static BigDecimal partMax(String part) {
        BigDecimal[] range = PART_RANGE.get(part);
        return range == null ? BigDecimal.valueOf(200) : range[1];
    }

    /** 尺寸是否落在合理区间内 */
    public static boolean validPart(String part, BigDecimal value) {
        if (value == null) {
            return false;
        }
        return value.compareTo(partMin(part)) >= 0 && value.compareTo(partMax(part)) <= 0;
    }

    /** 给前端用的「部位 → 区间」字典 */
    public static Map<String, Map<String, BigDecimal>> partRanges() {
        Map<String, Map<String, BigDecimal>> result = new LinkedHashMap<>();
        PART_RANGE.forEach((part, range) -> {
            Map<String, BigDecimal> pair = new LinkedHashMap<>();
            pair.put("min", range[0]);
            pair.put("max", range[1]);
            result.put(part, pair);
        });
        return result;
    }

    public static boolean isValidOrderStatus(String status) {
        return status != null && ORDER_STATUS.containsKey(status);
    }

    public static String orderStatusName(String status) {
        return status == null ? "" : ORDER_STATUS.getOrDefault(status, status);
    }

    public static Map<String, String> orderStatuses() {
        return ORDER_STATUS;
    }

    public static List<String> orderStatusOrder() {
        return ORDER_STATUS_ORDER;
    }

    /** 订单状态的序号，0 起 */
    public static int orderStatusIndex(String status) {
        int index = ORDER_STATUS_ORDER.indexOf(status);
        return index < 0 ? -1 : index;
    }

    /** 下一状态，已是终态则返回 null */
    public static String nextOrderStatus(String status) {
        int index = orderStatusIndex(status);
        if (index < 0 || index >= ORDER_STATUS_ORDER.size() - 1) {
            return null;
        }
        return ORDER_STATUS_ORDER.get(index + 1);
    }

    public static boolean isValidStep(String step) {
        return step != null && STEP.containsKey(step);
    }

    public static String stepName(String step) {
        return step == null ? "" : STEP.getOrDefault(step, step);
    }

    public static Map<String, String> steps() {
        return STEP;
    }

    public static boolean isValidWoStatus(String status) {
        return status != null && WO_STATUS.containsKey(status);
    }

    public static String woStatusName(String status) {
        return status == null ? "" : WO_STATUS.getOrDefault(status, status);
    }

    public static List<String> stepSequence() {
        return STEP_SEQUENCE;
    }

    public static String stepOwner(String step) {
        return STEP_OWNER.getOrDefault(step, "未指派");
    }

    public static BigDecimal stepHours(String step) {
        return STEP_HOURS.getOrDefault(step, BigDecimal.ONE);
    }

    public static Map<String, String> woStatuses() {
        return WO_STATUS;
    }
}
