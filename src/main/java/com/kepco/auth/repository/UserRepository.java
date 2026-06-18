package com.kepco.auth.repository;

import com.kepco.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // ⭕ 기존 로그인 ID 단건 조회
    Optional<User> findByLoginId(String loginId);

    /**
     * 💡 [MIS 중심의 임직원 전체 목록 조회]
     * 1. LEFT JOIN FETCH를 통해 recovery_worker 프로필이 없는 순수 내근직(ADMIN, HR 등) 사원까지 완벽 포함
     * 2. 'ROLE_CITIZEN'이 아닌 모든 역할을 임직원으로 간주하여 향후 추가될 사원 직무 권한까지 자동 수용 (확장성 확보)
     * 3. 최근 입사일(hiredAt) 및 최근 등록 순으로 정렬하여 프론트엔드 테이블 노출 최적화
     */
    @Query("SELECT u FROM User u " +
           "LEFT JOIN FETCH u.recoveryWorker " +
           "WHERE u.role != 'ROLE_CITIZEN' " +
           "ORDER BY u.hiredAt DESC, u.id DESC")
    List<User> findAllEmployeesWithProfile();
}
