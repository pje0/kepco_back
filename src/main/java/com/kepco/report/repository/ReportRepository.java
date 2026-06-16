package com.kepco.report.repository;

import com.kepco.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    // 시민(citizenId) 본인의 민원 목록을 최신순으로 정렬하여 조회
    List<Report> findAllByCitizenIdOrderByCreatedAtDesc(Long citizenId);
}