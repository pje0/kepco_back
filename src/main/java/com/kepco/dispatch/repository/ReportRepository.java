package com.kepco.dispatch.repository;

import com.kepco.report.entity.Report;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    // 📊 상단 카운트용
    long countByStatus(String status);

    // 🔍 미배정 대기 신고 리스트
    List<Report> findByStatus(String status);

    /**
     * 🚨 신고 상태 변경
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
        UPDATE Report r
        SET r.status = :status
        WHERE r.id = :reportId
    """)
    int updateStatus(
            @Param("reportId") Long reportId,
            @Param("status") String status
    );
}