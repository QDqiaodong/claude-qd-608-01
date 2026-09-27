package com.garment.factory.repository;

import java.math.BigDecimal;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.garment.factory.domain.Fabric;
import com.garment.factory.dto.FabricCardDto;

/**
 * 面料库存：卡片墙走 JPQL 构造器表达式投影 + Slice 滚动分页，低库存标记在 JPQL 里算。
 */
public interface FabricRepository extends JpaRepository<Fabric, Long> {

    @Query("SELECT new com.garment.factory.dto.FabricCardDto("
            + "f.id, f.fabricNo, f.name, f.composition, f.color, f.colorHex, f.stockMeters, f.unitPrice, f.supplier, "
            + "CASE WHEN f.stockMeters < :lowLine THEN TRUE ELSE FALSE END) "
            + "FROM Fabric f "
            + "WHERE (:composition IS NULL OR :composition = '' OR f.composition = :composition) "
            + "  AND (:lowOnly = FALSE OR f.stockMeters < :lowLine) "
            + "  AND (:keyword IS NULL OR :keyword = '' "
            + "       OR f.name LIKE CONCAT('%', :keyword, '%') "
            + "       OR f.fabricNo LIKE CONCAT('%', :keyword, '%') "
            + "       OR f.supplier LIKE CONCAT('%', :keyword, '%')) "
            + "ORDER BY f.stockMeters ASC, f.fabricNo ASC")
    Slice<FabricCardDto> sliceBy(@Param("composition") String composition,
                                 @Param("lowOnly") Boolean lowOnly,
                                 @Param("keyword") String keyword,
                                 @Param("lowLine") BigDecimal lowLine,
                                 Pageable pageable);

    @Modifying
    @Query("UPDATE Fabric f SET f.stockMeters = f.stockMeters + :delta WHERE f.id = :id")
    int restock(@Param("id") Long id, @Param("delta") BigDecimal delta);

    boolean existsByFabricNo(String fabricNo);

    boolean existsByFabricNoAndIdNot(String fabricNo, Long id);
}
