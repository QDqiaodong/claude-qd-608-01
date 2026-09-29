package com.garment.factory.grading.engine;

import java.math.BigDecimal;

/**
 * 分段规则的一个区段。
 *
 * <p>级差以「边」描述：边 k 连接级差 k 与 k+1（k 为较小端的级差）。
 * 升码经过边 k 时，部位变化 {@code deltaMicrons}；降码反向经过则减去该量。
 * 例如区段 fromGrade=-2, toGrade=0 覆盖边 [-2→-1] 与 [-1→0]。
 */
public class Segment {

    /** 覆盖边的较小端级差（含）。 */
    private int fromGrade;

    /** 覆盖边的较大端级差（含）；toGrade 必须 ≥ fromGrade+1。 */
    private int toGrade;

    /** 该区段每条边的变化量（内部微米，可为负）。 */
    private BigDecimal deltaMicrons;

    public Segment() {
    }

    public Segment(int fromGrade, int toGrade, BigDecimal deltaMicrons) {
        this.fromGrade = fromGrade;
        this.toGrade = toGrade;
        this.deltaMicrons = deltaMicrons;
    }

    /** 该区段覆盖的边是否包含边 edge（edge 为较小端级差）。 */
    public boolean coversEdge(int edge) {
        return edge >= fromGrade && edge <= toGrade - 1;
    }

    public int getFromGrade() {
        return fromGrade;
    }

    public void setFromGrade(int fromGrade) {
        this.fromGrade = fromGrade;
    }

    public int getToGrade() {
        return toGrade;
    }

    public void setToGrade(int toGrade) {
        this.toGrade = toGrade;
    }

    public BigDecimal getDeltaMicrons() {
        return deltaMicrons;
    }

    public void setDeltaMicrons(BigDecimal deltaMicrons) {
        this.deltaMicrons = deltaMicrons;
    }
}
