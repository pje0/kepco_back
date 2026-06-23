package com.kepco.dispatch.repository;

import com.kepco.dispatch.entity.Dispatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph; // 🚨 임포트 추가
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DispatchRepository extends JpaRepository<Dispatch, Long> {
    
    long countByStatus(String status);
    List<Dispatch> findAllByOrderByIdDesc();

    /**
     * ⚡ [최종 튜닝 완료] 페이징 카운트 분리 및 엔티티 그래프 결합형 고속 검색 엔진
     * - @EntityGraph(attributePaths = {"complaint"}) 를 장착하여 하이버네이트 지연 로딩 껍데기(ByteBuddy) 에러를 원천 박멸합니다.
     * - countQuery 명시적 분리를 유지하여 PostgreSQL 부분 복합 인덱스 스캔 효율을 100% 가동합니다.
     */
    @EntityGraph(attributePaths = {"complaint"}) // 🎯 [핵심 교정]: complaint 알맹이를 프록시가 아닌 온전한 객체로 한 번에 묶어 가져옵니다.
    @Query(value = """
        SELECT d FROM Dispatch d 
        WHERE d.status = 'resolved'
          AND d.completedAt BETWEEN :startDate AND :endDate
          AND (:region IS NULL OR d.complaint.region = :region)
          AND (:district IS NULL OR d.complaint.district = :district)
          AND (:aiCategory IS NULL OR d.complaint.aiCategory = :aiCategory)
          AND (:aiPriority IS NULL OR d.complaint.aiPriority = :aiPriority)
          AND (:citizenId IS NULL OR d.complaint.citizenId = :citizenId)
    """,
    countQuery = """
        SELECT COUNT(d) FROM Dispatch d 
        WHERE d.status = 'resolved'
          AND d.completedAt BETWEEN :startDate AND :endDate
          AND (:region IS NULL OR d.complaint.region = :region)
          AND (:district IS NULL OR d.complaint.district = :district)
          AND (:aiCategory IS NULL OR d.complaint.aiCategory = :aiCategory)
          AND (:aiPriority IS NULL OR d.complaint.aiPriority = :aiPriority)
          AND (:citizenId IS NULL OR d.complaint.citizenId = :citizenId)
    """)
    Page<Dispatch> searchHistory(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("region") String region,
            @Param("district") String district,
            @Param("aiCategory") String aiCategory,
            @Param("aiPriority") String aiPriority,
            @Param("citizenId") Long citizenId,
            Pageable pageable
    );
}
