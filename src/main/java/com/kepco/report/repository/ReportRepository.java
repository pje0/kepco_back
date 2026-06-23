package com.kepco.report.repository;

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

    // =========================================================================
    // 🧑‍💻 [시민/파견팀 공통] 기본 조회 및 상태 변경 영역
    // =========================================================================

    // 시민(citizenId) 본인의 민원 목록을 최신순으로 정렬하여 조회
    List<Report> findAllByCitizenIdOrderByCreatedAtDesc(Long citizenId);

    // 🔍 미배정 대기 신고 리스트 (파견팀 조회용)
    List<Report> findByStatus(String status);

    // 🚨 신고 상태 변경 (파견팀 배정 및 처리 상태 업데이트용)
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


    // =========================================================================
    // 📊 [총괄 관리자] 대시보드 통계 전용 영역
    // =========================================================================

    // 🚨 [대시보드용] 상태별 건수 조회 (상단 KPI 카운트용)
    long countByStatus(String status);

    // 🚨 [대시보드용] 통계 투영(Projection) 인터페이스
    interface StatCount {
        String getName();
        Long getValue();
    }

    // 🚨 [대시보드용] 지역별 민원 건수 통계 (Group By)
    @Query("SELECT c.district AS name, COUNT(c) AS value FROM Report c GROUP BY c.district")
    List<StatCount> countReportsByDistrict();

    // 🚨 [대시보드용] 카테고리별 민원 건수 통계 (Group By)
    @Query("SELECT c.aiCategory AS name, COUNT(c) AS value FROM Report c WHERE c.aiCategory IS NOT NULL GROUP BY c.aiCategory")
    List<StatCount> countReportsByCategory();
    
    // 🚨 [대시보드용] 월별 민원 건수 통계 (PostgreSQL 전용 쿼리)
    @Query(value = "SELECT TO_CHAR(created_at, 'YYYY-MM') AS name, COUNT(*) AS value " +
                   "FROM complaint " +
                   "WHERE created_at >= NOW() - INTERVAL '6 months' " +
                   "GROUP BY TO_CHAR(created_at, 'YYYY-MM') " +
                   "ORDER BY name ASC", nativeQuery = true)
    List<StatCount> countReportsByMonth();

    // =========================================================================
    // 🧠 [인공지능 코어] 실시간 민원 분류 자동 적재 전용 영역
    // =========================================================================
    
    /**
     * 🚨 [JPA 무결성 수복 벌크 메서드]: 엔티티 소스 코드 수정 없이 
     * DB complaint 테이블의 ai_category, ai_priority 컬럼을 초고속 실시간 적재합니다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
        UPDATE Report r 
        SET r.aiCategory = :aiCategory, r.aiPriority = :aiPriority 
        WHERE r.id = :reportId
    """)
    void updateAiClassification(
            @Param("reportId") Long reportId, 
            @Param("aiCategory") String aiCategory, 
            @Param("aiPriority") String aiPriority
    );
}
