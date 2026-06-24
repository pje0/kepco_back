package com.kepco.work.repository;

import com.kepco.auth.entity.RecoveryWorker;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface WorkWorkerRepository extends JpaRepository<RecoveryWorker, Long> {
    // 로그인한 User의 username으로 RecoveryWorker 찾기
    Optional<RecoveryWorker> findByUser_Username(String username);
}