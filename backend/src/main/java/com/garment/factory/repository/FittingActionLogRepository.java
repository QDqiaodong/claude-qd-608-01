package com.garment.factory.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.garment.factory.domain.FittingActionLog;

/**
 * 试衣整改操作流水：只追加，不修改。问题详情/订单汇总都按时间正序回放。
 */
public interface FittingActionLogRepository extends JpaRepository<FittingActionLog, Long> {

    List<FittingActionLog> findByIssueIdOrderByActedAtAscIdAsc(Long issueId);

    List<FittingActionLog> findByOrderIdOrderByActedAtAscIdAsc(Long orderId);
}
