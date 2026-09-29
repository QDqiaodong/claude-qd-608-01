package com.garment.factory.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * 放码方案：一个基准尺码 + 目标尺码序列 + 分部位放码规则，以及服务端生成的完整结果快照。
 *
 * 留存口径：
 *   - 规则以毫米规范化后入库（rules_json），input_unit 只记录版师录入时的单位，回显时换算；
 *   - snapshot_json 是服务端一次算完的完整结果（不可变快照），重新打开直接读取，保证数值一致；
 *   - legacy_flag=1 为旧厘米数据：按兼容规则（round_step_mm=0.1）读入，重算仍为旧的两位小数厘米结果。
 */
@Entity
@Table(name = "grade_scheme")
public class GradeScheme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scheme_no", nullable = false, length = 30)
    private String schemeNo;

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    /** 关联款式，可空（临时计算方案不绑定款式） */
    @Column(name = "style_id")
    private Long styleId;

    /** SAVED=已留存；临时方案不落库 */
    @Column(name = "status", nullable = false, length = 10)
    private String status;

    @Column(name = "base_size_code", nullable = false, length = 20)
    private String baseSizeCode;

    /** 录入单位 CM/MM，仅影响录入与回显，内部一律毫米 */
    @Column(name = "input_unit", nullable = false, length = 4)
    private String inputUnit;

    /** 统一舍入步进（毫米），所有部位只按它做一次 HALF_UP 舍入 */
    @Column(name = "round_step_mm", nullable = false, precision = 8, scale = 2)
    private BigDecimal roundStepMm;

    /** 有序尺码序列 JSON，如 ["XS","S","M","L","XL"] */
    @Lob
    @Column(name = "sizes_json", nullable = false)
    private String sizesJson;

    /** 基准尺寸 JSON（毫米规范值），元素 {part, side, valueMm} */
    @Lob
    @Column(name = "bases_json", nullable = false)
    private String basesJson;

    /** 放码规则 JSON（毫米规范参数） */
    @Lob
    @Column(name = "rules_json", nullable = false)
    private String rulesJson;

    /** 完整结果快照 JSON：全部尺码 × 部位（含侧别）的毫米/厘米结果、相对基准总变化、异常定位 */
    @Lob
    @Column(name = "snapshot_json", nullable = false)
    private String snapshotJson;

    /** 旧数据兼容标记：1=仅厘米小数的旧方案 */
    @Column(name = "legacy_flag", nullable = false)
    private Boolean legacyFlag;

    /** 结果版本：每次重新生成 +1，用于确认打开的是同一份结果 */
    @Column(name = "result_version", nullable = false)
    private Integer resultVersion;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSchemeNo() {
        return schemeNo;
    }

    public void setSchemeNo(String schemeNo) {
        this.schemeNo = schemeNo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getStyleId() {
        return styleId;
    }

    public void setStyleId(Long styleId) {
        this.styleId = styleId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBaseSizeCode() {
        return baseSizeCode;
    }

    public void setBaseSizeCode(String baseSizeCode) {
        this.baseSizeCode = baseSizeCode;
    }

    public String getInputUnit() {
        return inputUnit;
    }

    public void setInputUnit(String inputUnit) {
        this.inputUnit = inputUnit;
    }

    public BigDecimal getRoundStepMm() {
        return roundStepMm;
    }

    public void setRoundStepMm(BigDecimal roundStepMm) {
        this.roundStepMm = roundStepMm;
    }

    public String getSizesJson() {
        return sizesJson;
    }

    public void setSizesJson(String sizesJson) {
        this.sizesJson = sizesJson;
    }

    public String getBasesJson() {
        return basesJson;
    }

    public void setBasesJson(String basesJson) {
        this.basesJson = basesJson;
    }

    public String getRulesJson() {
        return rulesJson;
    }

    public void setRulesJson(String rulesJson) {
        this.rulesJson = rulesJson;
    }

    public String getSnapshotJson() {
        return snapshotJson;
    }

    public void setSnapshotJson(String snapshotJson) {
        this.snapshotJson = snapshotJson;
    }

    public Boolean getLegacyFlag() {
        return legacyFlag;
    }

    public void setLegacyFlag(Boolean legacyFlag) {
        this.legacyFlag = legacyFlag;
    }

    public Integer getResultVersion() {
        return resultVersion;
    }

    public void setResultVersion(Integer resultVersion) {
        this.resultVersion = resultVersion;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
