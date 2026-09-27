package com.garment.factory.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.garment.factory.domain.Measurement;
import com.garment.factory.dto.MeasurementListItemDto;

/**
 * 量体记录：列表走 JPQL 构造器表达式投影 + Slice 滚动分页。
 */
public interface MeasurementRepository extends JpaRepository<Measurement, Long> {

    @Query("SELECT new com.garment.factory.dto.MeasurementListItemDto("
            + "m.id, m.recordNo, m.customerId, m.customerName, m.tailorName, m.measureDate, "
            + "m.bust, m.waist, m.hip, m.shoulder, m.sleeveLength, m.bodyLength, m.pantsLength, m.remark) "
            + "FROM Measurement m "
            + "WHERE (:customerId IS NULL OR m.customerId = :customerId) "
            + "  AND (:tailorName IS NULL OR :tailorName = '' OR m.tailorName = :tailorName) "
            + "ORDER BY m.measureDate DESC, m.id DESC")
    Slice<MeasurementListItemDto> sliceBy(@Param("customerId") Long customerId,
                                          @Param("tailorName") String tailorName,
                                          Pageable pageable);

    @Query("SELECT new com.garment.factory.dto.MeasurementListItemDto("
            + "m.id, m.recordNo, m.customerId, m.customerName, m.tailorName, m.measureDate, "
            + "m.bust, m.waist, m.hip, m.shoulder, m.sleeveLength, m.bodyLength, m.pantsLength, m.remark) "
            + "FROM Measurement m WHERE m.customerId = :customerId "
            + "ORDER BY m.measureDate DESC, m.id DESC")
    Slice<MeasurementListItemDto> sliceByCustomer(@Param("customerId") Long customerId, Pageable pageable);

    boolean existsByRecordNo(String recordNo);

    boolean existsByRecordNoAndIdNot(String recordNo, Long id);

    long countByCustomerId(Long customerId);
}
