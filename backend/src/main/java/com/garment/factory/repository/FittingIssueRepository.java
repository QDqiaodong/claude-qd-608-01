package com.garment.factory.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.garment.factory.domain.FittingIssue;
import com.garment.factory.dto.FittingIssueListItemDto;

import jakarta.persistence.LockModeType;

/**
 * 试衣问题单：列表走 JPQL 构造器表达式投影；所有写流程先锁订单行，再用悲观行锁取问题单。
 */
public interface FittingIssueRepository extends JpaRepository<FittingIssue, Long> {

    @Query("SELECT new com.garment.factory.dto.FittingIssueListItemDto("
            + "i.id, i.issueNo, i.orderId, o.orderNo, c.name, "
            + "i.bodyPart, i.severity, i.description, i.responsibleStep, i.raisedBy, i.raisedAt, "
            + "i.status, i.released, i.closedAt, i.reopenCount, i.version) "
            + "FROM FittingIssue i "
            + "JOIN com.garment.factory.domain.CustomOrder o ON o.id = i.orderId "
            + "JOIN com.garment.factory.domain.Customer c ON c.id = o.customerId "
            + "WHERE (:orderId IS NULL OR i.orderId = :orderId) "
            + "  AND (:status IS NULL OR :status = '' OR i.status = :status) "
            + "  AND (:severity IS NULL OR :severity = '' OR i.severity = :severity) "
            + "  AND (:openOnly = FALSE OR i.status <> 'CLOSED') "
            + "  AND (:keyword IS NULL OR :keyword = '' "
            + "       OR i.issueNo LIKE CONCAT('%', :keyword, '%') "
            + "       OR o.orderNo LIKE CONCAT('%', :keyword, '%') "
            + "       OR c.name LIKE CONCAT('%', :keyword, '%')) "
            + "ORDER BY i.id DESC")
    Slice<FittingIssueListItemDto> sliceBy(@Param("orderId") Long orderId,
                                           @Param("status") String status,
                                           @Param("severity") String severity,
                                           @Param("openOnly") boolean openOnly,
                                           @Param("keyword") String keyword,
                                           Pageable pageable);

    /** 悲观行锁：并发写流程在锁到订单行后再锁问题单行，保证状态判断与落库在同一串行序列内 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM FittingIssue i WHERE i.id = :id")
    Optional<FittingIssue> findByIdForUpdate(@Param("id") Long id);

    List<FittingIssue> findByOrderIdOrderByIdAsc(Long orderId);

    long countByOrderId(Long orderId);

    boolean existsByIssueNo(String issueNo);
}
