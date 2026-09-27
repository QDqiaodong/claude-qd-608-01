package com.garment.factory.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.garment.factory.domain.CustomOrder;
import com.garment.factory.dto.OrderBoardCardDto;
import com.garment.factory.dto.OrderListItemDto;

import jakarta.persistence.LockModeType;

/**
 * 定制订单：列表与看板全部走 JPQL 构造器表达式投影，状态推进走 JPQL update。
 */
public interface CustomOrderRepository extends JpaRepository<CustomOrder, Long> {

    @Query("SELECT new com.garment.factory.dto.OrderListItemDto("
            + "o.id, o.orderNo, o.customerId, c.name, c.phone, "
            + "s.styleNo, s.name, s.category, f.fabricNo, f.name, f.color, f.colorHex, "
            + "o.measurementId, m.recordNo, o.usageMeters, o.orderDate, o.deliveryDate, "
            + "o.fabricCost, o.craftFee, o.totalPrice, o.deposit, o.balance, o.depositPaid, o.balancePaid, "
            + "o.status, o.remark) "
            + "FROM CustomOrder o "
            + "JOIN com.garment.factory.domain.Customer c ON c.id = o.customerId "
            + "JOIN com.garment.factory.domain.Style s ON s.id = o.styleId "
            + "JOIN com.garment.factory.domain.Fabric f ON f.id = o.fabricId "
            + "LEFT JOIN com.garment.factory.domain.Measurement m ON m.id = o.measurementId "
            + "WHERE (:status IS NULL OR :status = '' OR o.status = :status) "
            + "  AND (:customerId IS NULL OR o.customerId = :customerId) "
            + "  AND (:keyword IS NULL OR :keyword = '' "
            + "       OR o.orderNo LIKE CONCAT('%', :keyword, '%') "
            + "       OR c.name LIKE CONCAT('%', :keyword, '%') "
            + "       OR s.name LIKE CONCAT('%', :keyword, '%')) "
            + "ORDER BY o.deliveryDate ASC, o.id DESC")
    Slice<OrderListItemDto> sliceBy(@Param("status") String status,
                                    @Param("customerId") Long customerId,
                                    @Param("keyword") String keyword,
                                    Pageable pageable);

    @Query("SELECT new com.garment.factory.dto.OrderBoardCardDto("
            + "o.id, o.orderNo, c.name, s.name, f.name, f.colorHex, m.tailorName, o.deliveryDate, "
            + "o.totalPrice, o.usageMeters, o.status, o.balancePaid) "
            + "FROM CustomOrder o "
            + "JOIN com.garment.factory.domain.Customer c ON c.id = o.customerId "
            + "JOIN com.garment.factory.domain.Style s ON s.id = o.styleId "
            + "JOIN com.garment.factory.domain.Fabric f ON f.id = o.fabricId "
            + "LEFT JOIN com.garment.factory.domain.Measurement m ON m.id = o.measurementId "
            + "ORDER BY o.deliveryDate ASC, o.id DESC")
    List<OrderBoardCardDto> findBoardCards();

    @Modifying
    @Query("UPDATE CustomOrder o SET o.status = :status WHERE o.id = :id")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /** 整改相关写流程统一先锁订单行：串行化同订单的所有操作，并统一加锁顺序避免死锁 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM CustomOrder o WHERE o.id = :id")
    Optional<CustomOrder> findByIdForUpdate(@Param("id") Long id);

    @Modifying
    @Query("UPDATE CustomOrder o SET o.balancePaid = TRUE WHERE o.id = :id")
    int payBalance(@Param("id") Long id);

    @Modifying
    @Query("UPDATE CustomOrder o SET o.measurementId = :measurementId WHERE o.id = :id")
    int bindMeasurement(@Param("id") Long id, @Param("measurementId") Long measurementId);

    long countByStyleId(Long styleId);

    long countByFabricId(Long fabricId);

    long countByCustomerId(Long customerId);

    boolean existsByOrderNo(String orderNo);

    List<CustomOrder> findByDeliveryDateBeforeAndStatusNot(LocalDate date, String status);
}
