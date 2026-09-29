package com.garment.factory.grading.doc;

import java.util.ArrayList;
import java.util.List;

/**
 * 放码方案 v2 文档（与 specJson 一一对应）。
 *
 * <p>所有长度输入都是<b>字符串</b>（避免数字浮点误差），并标注输入单位：
 * 同一方案内部位数值与增量可以各自带单位（默认跟随方案 {@code inputUnit}）。
 * 引擎解析时全部精确折算成微米有理数量。
 */
public class SchemeDoc {

    /** 文档结构版本；旧数据为 1，新写固定 2。 */
    private Integer schemaVersion = 2;

    /** 基准尺码级差（0 = 基准，升码正数、降码负数）。 */
    private Integer baseGrade = 0;

    /** 页面录入/展示单位偏好：cm / mm（不影响内部数值，只影响写法）。 */
    private String inputUnit = "cm";

    /** 统一舍入口径。 */
    private RoundingDoc rounding = new RoundingDoc();

    /** 目标尺码序列。 */
    private List<SizeDoc> sizes = new ArrayList<>();

    /** 部位槽位。 */
    private List<PartDoc> parts = new ArrayList<>();

    /** 跨部位比例检查。 */
    private List<RatioCheckDoc> ratioChecks = new ArrayList<>();

    public static class SizeDoc {
        private Integer grade;
        private String name;

        public Integer getGrade() {
            return grade;
        }

        public void setGrade(Integer grade) {
            this.grade = grade;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class RoundingDoc {
        /** 舍入单位 cm/mm。 */
        private String unit = "cm";
        /** 步进，如 0.1 / 0.5 / 1（cm），或 1 / 5（mm）。 */
        private String step = "0.1";

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public String getStep() {
            return step;
        }

        public void setStep(String step) {
            this.step = step;
        }
    }

    public static class PartDoc {
        private String partId;
        private String partName;
        /** 侧别：null/L/R。 */
        private String side;
        /** 基准尺寸（字符串）。 */
        private String base;
        /** 该数值所用单位，缺省跟随方案 inputUnit。 */
        private String unit;
        /** 放码方向 UP/DOWN/ANY。 */
        private String direction = "UP";
        /** 规则。 */
        private RuleDoc rule = new RuleDoc();

        public String getPartId() {
            return partId;
        }

        public void setPartId(String partId) {
            this.partId = partId;
        }

        public String getPartName() {
            return partName;
        }

        public void setPartName(String partName) {
            this.partName = partName;
        }

        public String getSide() {
            return side;
        }

        public void setSide(String side) {
            this.side = side;
        }

        public String getBase() {
            return base;
        }

        public void setBase(String base) {
            this.base = base;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public String getDirection() {
            return direction;
        }

        public void setDirection(String direction) {
            this.direction = direction;
        }

        public RuleDoc getRule() {
            return rule;
        }

        public void setRule(RuleDoc rule) {
            this.rule = rule;
        }
    }

    public static class RuleDoc {
        /** FIXED / RATIO / SEGMENT。 */
        private String type = "FIXED";
        /** FIXED：每级变化量。 */
        private String delta;
        /** 变化量单位，缺省跟随部位/方案。 */
        private String unit;
        /** RATIO：比例，"4%" 或 "0.04"。 */
        private String rate;
        /** SEGMENT：分段。 */
        private List<SegmentDoc> segments = new ArrayList<>();

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getDelta() {
            return delta;
        }

        public void setDelta(String delta) {
            this.delta = delta;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public String getRate() {
            return rate;
        }

        public void setRate(String rate) {
            this.rate = rate;
        }

        public List<SegmentDoc> getSegments() {
            return segments;
        }

        public void setSegments(List<SegmentDoc> segments) {
            this.segments = segments;
        }
    }

    public static class SegmentDoc {
        private Integer fromGrade;
        private Integer toGrade;
        private String delta;
        private String unit;

        public Integer getFromGrade() {
            return fromGrade;
        }

        public void setFromGrade(Integer fromGrade) {
            this.fromGrade = fromGrade;
        }

        public Integer getToGrade() {
            return toGrade;
        }

        public void setToGrade(Integer toGrade) {
            this.toGrade = toGrade;
        }

        public String getDelta() {
            return delta;
        }

        public void setDelta(String delta) {
            this.delta = delta;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }
    }

    public static class RatioCheckDoc {
        private String checkCode;
        private String numeratorSlot;
        private String denominatorSlot;
        private String minRatio;
        private String maxRatio;
        private String label;

        public String getCheckCode() {
            return checkCode;
        }

        public void setCheckCode(String checkCode) {
            this.checkCode = checkCode;
        }

        public String getNumeratorSlot() {
            return numeratorSlot;
        }

        public void setNumeratorSlot(String numeratorSlot) {
            this.numeratorSlot = numeratorSlot;
        }

        public String getDenominatorSlot() {
            return denominatorSlot;
        }

        public void setDenominatorSlot(String denominatorSlot) {
            this.denominatorSlot = denominatorSlot;
        }

        public String getMinRatio() {
            return minRatio;
        }

        public void setMinRatio(String minRatio) {
            this.minRatio = minRatio;
        }

        public String getMaxRatio() {
            return maxRatio;
        }

        public void setMaxRatio(String maxRatio) {
            this.maxRatio = maxRatio;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }

    public Integer getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(Integer schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public Integer getBaseGrade() {
        return baseGrade;
    }

    public void setBaseGrade(Integer baseGrade) {
        this.baseGrade = baseGrade;
    }

    public String getInputUnit() {
        return inputUnit;
    }

    public void setInputUnit(String inputUnit) {
        this.inputUnit = inputUnit;
    }

    public RoundingDoc getRounding() {
        return rounding;
    }

    public void setRounding(RoundingDoc rounding) {
        this.rounding = rounding;
    }

    public List<SizeDoc> getSizes() {
        return sizes;
    }

    public void setSizes(List<SizeDoc> sizes) {
        this.sizes = sizes;
    }

    public List<PartDoc> getParts() {
        return parts;
    }

    public void setParts(List<PartDoc> parts) {
        this.parts = parts;
    }

    public List<RatioCheckDoc> getRatioChecks() {
        return ratioChecks;
    }

    public void setRatioChecks(List<RatioCheckDoc> ratioChecks) {
        this.ratioChecks = ratioChecks;
    }
}
