package com.kepco.dispatch.repository;

import com.kepco.dispatch.entity.Dispatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DispatchRepository extends JpaRepository<Dispatch, Long> {
    
    // 📊 [상단 카운트용]: 파견 상태별 개수 조회 (assigned = 출동중, completed = 완료)
    long countByStatus(String status);

    // 📋 [하단 메인 테이블용]: 모든 파견 이력을 ID 최신순(역순)으로 조회
    List<Dispatch> findAllByOrderByIdDesc();
}
