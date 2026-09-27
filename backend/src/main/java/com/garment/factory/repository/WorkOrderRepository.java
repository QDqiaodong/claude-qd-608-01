package com.garment.factory.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.garment.factory.domain.WorkOrder;
import com.garment.factory.dto.WorkOrderListItemDto;

/**
 * 工序工单：列表走 JPQL 构造器表达式投影 + Slice；状态推进走 JPQL update。
 */
public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {

    @Query("SELECT new com.garment.factory.dto.WorkOrderListItemDto("
            + "w.id, w.woNo, w.orderId, o.orderNo, c.name, s.name, "
            + "w.step, w.owner, w.plannedHours, w.actualHours, w.status, w.remark) "
            + "FROM WorkOrder w "
            + "JOIN com.garment.factory.domain.CustomOrder o ON o.id = w.orderId "
            + "JOIN com.garment.factory.domain.Customer c ON c.id = o.customerId "
            + "JOIN com.garment.factory.domain.Style s ON s.id = o.styleId "
            + "WHERE (:orderId IS NULL OR w.orderId = :orderId) "
            + "  AND (:status IS NULL OR :status = '' OR w.status = :status) "
            + "  AND (:step IS NULL OR :step = '' OR w.step = :step) "
            + "  AND (:owner IS NULL OR :owner = '' OR w.owner = :owner) "
            + "  AND (:keyword IS NULL OR :keyword = '' "
            + "       OR w.woNo LIKE CONCAT('%', :keyword, '%') "
            + "       OR o.orderNo LIKE CONCAT('%', :keyword, '%') "
            + "       OR c.name LIKE CONCAT('%', :keyword, '%')) "
            + "ORDER BY w.id ASC")
    Slice<WorkOrderListItemDto> sliceBy(@Param("orderId") Long orderId,
                                        @Param("status") String status,
                                        @Param("step") String step,
                                        @Param("owner") String owner,
                                        @Param("keyword") String keyword,
                                        Pageable pageable);

    @Modifying
    @Query("UPDATE WorkOrder w SET w.status = :status WHERE w.id = :id")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Modifying
    @Query("UPDATE WorkOrder w SET w.actualHours = :hours, w.remark = :remark WHERE w.id = :id")
    int updateProgress(@Param("id") Long id, @Param("hours") BigDecimal hours, @Param("remark") String remark);

    boolean existsByWoNo(String woNo);

    boolean existsByWoNoAndIdNot(String woNo, Long id);

    long countByOrderId(Long orderId);

    long countByOrderIdAndStatus(Long orderId, String status);

    List<WorkOrder> findByOrderIdOrderByIdAsc(Long orderId);

    /** 同一订单同一工序不允许重复派工 */
    long countByOrderIdAndStep(Long orderId, String step);
}
