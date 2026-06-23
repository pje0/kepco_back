package com.kepco.dispatch.repository;

import com.kepco.auth.entity.RecoveryWorker;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecoveryWorkerRepository
        extends JpaRepository<RecoveryWorker, Long> {

    // 📊 가용 상태 요원 수 조회
    long countByWorkStatus(String workStatus);

    // 🔍 가용 상태 요원 리스트 조회
    List<RecoveryWorker> findByWorkStatus(String workStatus);

    /**
     * 🚨 작업자 상태 변경
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
        UPDATE RecoveryWorker rw
        SET rw.workStatus = :workStatus
        WHERE rw.id = :workerId
    """)
    int updateWorkStatus(
            @Param("workerId") Long workerId,
            @Param("workStatus") String workStatus
    );
}