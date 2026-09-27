package com.garment.factory.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.garment.factory.domain.Style;
import com.garment.factory.dto.StyleListItemDto;

/**
 * 款式：列表走 JPQL 构造器表达式投影 + Slice 滚动分页。
 */
public interface StyleRepository extends JpaRepository<Style, Long> {

    @Query("SELECT new com.garment.factory.dto.StyleListItemDto("
            + "s.id, s.styleNo, s.name, s.category, s.basePrice, s.craftLevel, s.onSale, s.remark) "
            + "FROM Style s "
            + "WHERE (:category IS NULL OR :category = '' OR s.category = :category) "
            + "  AND (:onSale IS NULL OR s.onSale = :onSale) "
            + "  AND (:keyword IS NULL OR :keyword = '' "
            + "       OR s.name LIKE CONCAT('%', :keyword, '%') "
            + "       OR s.styleNo LIKE CONCAT('%', :keyword, '%')) "
            + "ORDER BY s.styleNo ASC")
    Slice<StyleListItemDto> sliceBy(@Param("category") String category,
                                    @Param("onSale") Boolean onSale,
                                    @Param("keyword") String keyword,
                                    Pageable pageable);

    Style findByStyleNo(String styleNo);

    boolean existsByStyleNo(String styleNo);

    boolean existsByStyleNoAndIdNot(String styleNo, Long id);
}
