package com.garment.factory.grading.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * 放码方案（可关联现有款式，也可是临时计算方案）。
 *
 * <p>方案内容 {@code specJson} 自带 schemaVersion：
 * <ul>
 *   <li>v2（当前）：区分输入单位 cm/mm，含舍入口径、分段、左右侧、比例校验等；</li>
 *   <li>v1（旧数据）：只有厘米小数，按 {@code LegacySchemeMigrator} 的兼容规则读入，
 *       毫米模式上线不改变旧方案的任何结果。</li>
 * </ul>
 *
 * <p>{@code resultJson} 保存<b>上一次成功生成的完整结果</b>。任何一次生成因公式错误失败时，
 * Service 层不写入该字段，旧结果原样保留。
 */
@Entity
@Table(name = "grading_scheme")
public class GradingScheme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scheme_no", nullable = false, length = 40)
    private String schemeNo;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** 关联款式 id；临时方案为空。 */
    @Column(name = "style_id")
    private Long styleId;

    /** 是否临时方案（临时方案在列表中与留存方案区分展示）。 */
    @Column(name = "temporary", nullable = false)
    private Boolean temporary = Boolean.FALSE;

    /** 方案结构版本：1=旧版厘米小数，2=当前多单位版。 */
    @Column(name = "schema_version", nullable = false)
    private Integer schemaVersion = 2;

    /** 方案内容指纹（对规范化后的 specJson 取 SHA-256），用于确定性校验。 */
    @Column(name = "content_hash", length = 64)
    private String contentHash;

    /** 方案 JSON（尺码序列、部位、规则、舍入口径等）。 */
    @Lob
    @Column(name = "spec_json", nullable = false)
    private String specJson;

    /** 上一次成功生成的完整结果 JSON；从未生成时为空。 */
    @Lob
    @Column(name = "result_json")
    private String resultJson;

    @Column(name = "result_generated_at")
    private LocalDateTime resultGeneratedAt;

    @Column(name = "remark", length = 300)
    private String remark;

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

    public Boolean getTemporary() {
        return temporary;
    }

    public void setTemporary(Boolean temporary) {
        this.temporary = temporary;
    }

    public Integer getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(Integer schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public String getSpecJson() {
        return specJson;
    }

    public void setSpecJson(String specJson) {
        this.specJson = specJson;
    }

    public String getResultJson() {
        return resultJson;
    }

    public void setResultJson(String resultJson) {
        this.resultJson = resultJson;
    }

    public LocalDateTime getResultGeneratedAt() {
        return resultGeneratedAt;
    }

    public void setResultGeneratedAt(LocalDateTime resultGeneratedAt) {
        this.resultGeneratedAt = resultGeneratedAt;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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
