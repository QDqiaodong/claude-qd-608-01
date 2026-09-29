package com.garment.factory.grading.doc;

import java.util.List;

/**
 * 一次放码生成的完整结果文档（resultJson）。
 * 全部长度以<b>微米字符串</b>持久化（内部口径，确定性、无浮点），
 * 同时冗余 cm/mm 两种显示值（由微米精确换算，不是重新舍入）。
 */
public class ResultDoc {

    private int baseGrade;
    private String roundingUnit;
    private String roundingStep;
    /** 量子（微米）字符串。 */
    private String quantumMicrons;
    private List<SizeView> sizes;
    private List<SlotView> slots;
    private List<AnomalyView> anomalies;
    private Meta meta;
    /** 结果是否已过期（方案内容在生成后又被修改）；true 时提示版师重新生成。 */
    private boolean stale;

    public static class SizeView {
        private int grade;
        private String name;

        public int getGrade() {
            return grade;
        }

        public void setGrade(int grade) {
            this.grade = grade;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class SlotView {
        private String slotKey;
        private String partId;
        private String partName;
        private String side;
        private String formulaSignature;
        private List<CellView> cells;

        public String getSlotKey() {
            return slotKey;
        }

        public void setSlotKey(String slotKey) {
            this.slotKey = slotKey;
        }

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

        public String getFormulaSignature() {
            return formulaSignature;
        }

        public void setFormulaSignature(String formulaSignature) {
            this.formulaSignature = formulaSignature;
        }

        public List<CellView> getCells() {
            return cells;
        }

        public void setCells(List<CellView> cells) {
            this.cells = cells;
        }
    }

    public static class CellView {
        private int grade;
        private String sizeName;
        /** 最终尺寸（微米）。 */
        private String finalMicrons;
        /** 未舍入尺寸（微米，有理数量字符串）。 */
        private String rawMicrons;
        /** 相对基准版的总变化（微米）。 */
        private String baseDeltaMicrons;
        /** 最终尺寸的 cm/mm 精确显示。 */
        private String cm;
        private String mm;
        private String deltaCm;
        private String deltaMm;

        public int getGrade() {
            return grade;
        }

        public void setGrade(int grade) {
            this.grade = grade;
        }

        public String getSizeName() {
            return sizeName;
        }

        public void setSizeName(String sizeName) {
            this.sizeName = sizeName;
        }

        public String getFinalMicrons() {
            return finalMicrons;
        }

        public void setFinalMicrons(String finalMicrons) {
            this.finalMicrons = finalMicrons;
        }

        public String getRawMicrons() {
            return rawMicrons;
        }

        public void setRawMicrons(String rawMicrons) {
            this.rawMicrons = rawMicrons;
        }

        public String getBaseDeltaMicrons() {
            return baseDeltaMicrons;
        }

        public void setBaseDeltaMicrons(String baseDeltaMicrons) {
            this.baseDeltaMicrons = baseDeltaMicrons;
        }

        public String getCm() {
            return cm;
        }

        public void setCm(String cm) {
            this.cm = cm;
        }

        public String getMm() {
            return mm;
        }

        public void setMm(String mm) {
            this.mm = mm;
        }

        public String getDeltaCm() {
            return deltaCm;
        }

        public void setDeltaCm(String deltaCm) {
            this.deltaCm = deltaCm;
        }

        public String getDeltaMm() {
            return deltaMm;
        }

        public void setDeltaMm(String deltaMm) {
            this.deltaMm = deltaMm;
        }
    }

    public static class AnomalyView {
        private String type;
        private String typeLabel;
        private int grade;
        private String sizeName;
        private String slotKey;
        private String slotName;
        private Integer edgeFromGrade;
        private Integer edgeToGrade;
        private String reason;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getTypeLabel() {
            return typeLabel;
        }

        public void setTypeLabel(String typeLabel) {
            this.typeLabel = typeLabel;
        }

        public int getGrade() {
            return grade;
        }

        public void setGrade(int grade) {
            this.grade = grade;
        }

        public String getSizeName() {
            return sizeName;
        }

        public void setSizeName(String sizeName) {
            this.sizeName = sizeName;
        }

        public String getSlotKey() {
            return slotKey;
        }

        public void setSlotKey(String slotKey) {
            this.slotKey = slotKey;
        }

        public String getSlotName() {
            return slotName;
        }

        public void setSlotName(String slotName) {
            this.slotName = slotName;
        }

        public Integer getEdgeFromGrade() {
            return edgeFromGrade;
        }

        public void setEdgeFromGrade(Integer edgeFromGrade) {
            this.edgeFromGrade = edgeFromGrade;
        }

        public Integer getEdgeToGrade() {
            return edgeToGrade;
        }

        public void setEdgeToGrade(Integer edgeToGrade) {
            this.edgeToGrade = edgeToGrade;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    public static class Meta {
        /** 生成所用方案的内容指纹，保证留档再开同值。 */
        private String specHash;
        private long elapsedMicros;
        private int cellCount;

        public String getSpecHash() {
            return specHash;
        }

        public void setSpecHash(String specHash) {
            this.specHash = specHash;
        }

        public long getElapsedMicros() {
            return elapsedMicros;
        }

        public void setElapsedMicros(long elapsedMicros) {
            this.elapsedMicros = elapsedMicros;
        }

        public int getCellCount() {
            return cellCount;
        }

        public void setCellCount(int cellCount) {
            this.cellCount = cellCount;
        }
    }

    public int getBaseGrade() {
        return baseGrade;
    }

    public void setBaseGrade(int baseGrade) {
        this.baseGrade = baseGrade;
    }

    public String getRoundingUnit() {
        return roundingUnit;
    }

    public void setRoundingUnit(String roundingUnit) {
        this.roundingUnit = roundingUnit;
    }

    public String getRoundingStep() {
        return roundingStep;
    }

    public void setRoundingStep(String roundingStep) {
        this.roundingStep = roundingStep;
    }

    public String getQuantumMicrons() {
        return quantumMicrons;
    }

    public void setQuantumMicrons(String quantumMicrons) {
        this.quantumMicrons = quantumMicrons;
    }

    public List<SizeView> getSizes() {
        return sizes;
    }

    public void setSizes(List<SizeView> sizes) {
        this.sizes = sizes;
    }

    public List<SlotView> getSlots() {
        return slots;
    }

    public void setSlots(List<SlotView> slots) {
        this.slots = slots;
    }

    public List<AnomalyView> getAnomalies() {
        return anomalies;
    }

    public void setAnomalies(List<AnomalyView> anomalies) {
        this.anomalies = anomalies;
    }

    public Meta getMeta() {
        return meta;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }

    public boolean isStale() {
        return stale;
    }

    public void setStale(boolean stale) {
        this.stale = stale;
    }
}
