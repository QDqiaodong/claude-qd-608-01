package com.garment.factory.service.grade;

import java.math.BigDecimal;

/**
 * 分段规则的一个码段（毫米规范参数）。
 * 级序以基准码为 0：fromLevel/toLevel 为绝对级序区间（含端点），
 * deltaMm 为该段内每升一级的增量（带符号）。
 */
public record NormSegment(Integer fromLevel, Integer toLevel, BigDecimal deltaMm) {
}
