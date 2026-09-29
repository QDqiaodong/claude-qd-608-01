package com.garment.factory.grading.repo;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.garment.factory.grading.model.GradingScheme;

public interface GradingSchemeRepository extends JpaRepository<GradingScheme, Long> {

    Page<GradingScheme> findByTemporary(Boolean temporary, Pageable pageable);

    Page<GradingScheme> findByTemporaryAndNameContaining(Boolean temporary, String name, Pageable pageable);

    List<GradingScheme> findByStyleId(Long styleId);

    boolean existsBySchemeNo(String schemeNo);
}
