package com.garment.factory.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.garment.factory.domain.ReworkRound;

/**
 * 试衣返修轮次：独立工单，不覆盖原工序工单；同一问题的当前有效轮次靠库表唯一索引兜底。
 */
public interface ReworkRoundRepository extends JpaRepository<ReworkRound, Long> {

    List<ReworkRound> findByIssueIdOrderByRoundSeqAscIdAsc(Long issueId);

    List<ReworkRound> findByOrderIdOrderByIdAsc(Long orderId);

    /** 当前有效返修工单：RUNNING/DONE 视为仍在返修闭环中（PASSED/REJECTED 已终结） */
    @Query("SELECT r FROM ReworkRound r WHERE r.issueId = :issueId AND r.status IN ('RUNNING','DONE')")
    List<ReworkRound> findActiveByIssueId(@Param("issueId") Long issueId);

    @Query("SELECT COUNT(r) FROM ReworkRound r WHERE r.orderId = :orderId AND r.status IN ('RUNNING','DONE')")
    long countActiveByOrderId(@Param("orderId") Long orderId);

    long countByOrderId(Long orderId);

    boolean existsByRoundNo(String roundNo);
}
