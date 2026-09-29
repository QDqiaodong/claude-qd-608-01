package com.garment.factory.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.garment.factory.domain.GradeScheme;

public interface GradeSchemeRepository extends JpaRepository<GradeScheme, Long> {

    boolean existsBySchemeNo(String schemeNo);

    boolean existsBySchemeNoAndIdNot(String schemeNo, Long id);

    List<GradeScheme> findAllByOrderByUpdatedAtDesc();
}
