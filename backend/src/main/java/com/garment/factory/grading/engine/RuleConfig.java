package com.garment.factory.grading.engine;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 一条放码规则（绑定到「部位 × 左右侧」槽位）。
 *
 * <p>所有长度字段在进入引擎前已折算成<b>微米有理数量</b>，比例为无量纲 BigDecimal。
 */
public class RuleConfig {

    private RuleType type;

    /** FIXED：每级变化量（μm，可为负）。 */
    private BigDecimal deltaMicrons;

    /** RATIO：每级比例（0.04 表示 4%）。 */
    private BigDecimal rate;

    /** SEGMENT：分段增量。 */
    private List<Segment> segments = new ArrayList<>();

    /**
     * SEGMENT 规则的「边 → 增量」索引（边 k 连接级差 k 与 k+1）。
     * 由 {@link com.garment.factory.grading.engine.GradingGenerator} 在结构校验阶段一次性构建，
     * 让每格放码从 O(段数) 扫描降为 O(1) 查表；瞬态、不参与 JSON / 指纹。
     */
    private transient java.util.Map<Integer, BigDecimal> edgeDeltaIndex;

    public RuleType getType() {
        return type;
    }

    public void setType(RuleType type) {
        this.type = type;
    }

    public BigDecimal getDeltaMicrons() {
        return deltaMicrons;
    }

    public void setDeltaMicrons(BigDecimal deltaMicrons) {
        this.deltaMicrons = deltaMicrons;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public List<Segment> getSegments() {
        return segments;
    }

    public void setSegments(List<Segment> segments) {
        this.segments = segments == null ? new ArrayList<>() : segments;
    }

    public java.util.Map<Integer, BigDecimal> getEdgeDeltaIndex() {
        return edgeDeltaIndex;
    }

    public void setEdgeDeltaIndex(java.util.Map<Integer, BigDecimal> edgeDeltaIndex) {
        this.edgeDeltaIndex = edgeDeltaIndex;
    }
}
