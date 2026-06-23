package com.kepco.dispatch.repository;

import com.kepco.dispatch.entity.Dispatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph; 
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
     * ⚡ [최종 개혁 완결] PostgreSQL 부분 인덱스 및 대문자 마이그레이션 싱크형 복합 검색 엔진
     * - 💡 [대문자 개혁]: totalElements 0건 전멸 결함을 진압하기 위해 d.status 조건절을 대문자 'RESOLVED'로 원천 세척!
     * - @EntityGraph 를 결합하여 지연 로딩 ByteBuddy 프록시 잔재 찌꺼기를 원천 박멸합니다.
     */
    @EntityGraph(attributePaths = {"complaint", "recoveryWorker", "recoveryWorker.user"}) 
    @Query(value = """
        SELECT d FROM Dispatch d 
        WHERE d.status = :status
          AND d.completedAt BETWEEN :startDate AND :endDate
          AND (:#{#req.region} IS NULL OR d.complaint.region = :#{#req.region})
          AND (:#{#req.district} IS NULL OR d.complaint.district = :#{#req.district})
          AND (:#{#req.aiCategory} IS NULL OR d.complaint.aiCategory = :#{#req.aiCategory})
          AND (:#{#req.aiPriority} IS NULL OR d.complaint.aiPriority = :#{#req.aiPriority})
          AND (:#{#req.citizenId} IS NULL OR d.complaint.citizenId = :#{#req.citizenId})
    """,
    countQuery = """
        SELECT COUNT(d) FROM Dispatch d 
        WHERE d.status = :status
          AND d.completedAt BETWEEN :startDate AND :endDate
          AND (:#{#req.region} IS NULL OR d.complaint.region = :#{#req.region})
          AND (:#{#req.district} IS NULL OR d.complaint.district = :#{#req.district})
          AND (:#{#req.aiCategory} IS NULL OR d.complaint.aiCategory = :#{#req.aiCategory})
          AND (:#{#req.aiPriority} IS NULL OR d.complaint.aiPriority = :#{#req.aiPriority})
          AND (:#{#req.citizenId} IS NULL OR d.complaint.citizenId = :#{#req.citizenId})
    """)
    Page<Dispatch> findAllByStatusAndCompletedAtBetween(
            @Param("status") String status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("req") com.kepco.dispatch.dto.HistorySearchRequestDto req,
            Pageable pageable
    );
}
