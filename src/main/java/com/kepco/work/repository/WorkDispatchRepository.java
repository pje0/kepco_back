package com.kepco.work.repository;

import com.kepco.dispatch.entity.Dispatch;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WorkDispatchRepository extends JpaRepository<Dispatch, Long> {
    // 특정 출동요원에게 배정된 출동만, 최신순
    List<Dispatch> findByRecoveryWorker_IdOrderByAssignedAtDesc(Long workerId);
}