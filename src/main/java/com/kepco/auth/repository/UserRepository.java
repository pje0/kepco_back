package com.kepco.auth.repository;

import com.kepco.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // 기존 로그인용 username 단건 조회
    Optional<User> findByUsername(String username);

    /**
     * 💡 [기존 코드 - 복구 완료]: 8번 기능용 임직원 전체 목록 조회
     * - FETCH JOIN을 통해 내근직/외근직을 한방에 긁어오며 N+1 성능 저하를 차단하던 원본 쿼리입니다.
     */
    @Query("SELECT u FROM User u " +
           "LEFT JOIN FETCH u.recoveryWorker " +
           "WHERE u.role != 'ROLE_CITIZEN' " +
           "ORDER BY u.hiredAt DESC, u.id DESC")
    List<User> findAllEmployeesWithProfile();

    /**
     * 💡 [신규 코드 - 유지]: 9번 기능용 서버사이드 페이징 + 동적 검색 고도화 쿼리
     * - 기존의 비즈니스 조건을 계승하면서 오프셋 페이징과 키워드(이름/부서/이메일/사번) 검색을 지원합니다.
     */
    @Query(value = "SELECT u FROM User u " +
                   "LEFT JOIN FETCH u.recoveryWorker " +
                   "WHERE u.role != 'ROLE_CITIZEN' " +
                   "AND (:search IS NULL OR :search = '' OR " +
                   "     LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                   "     LOWER(u.department) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                   "     LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                   "     LOWER(u.empNumber) LIKE LOWER(CONCAT('%', :search, '%')))",
           countQuery = "SELECT COUNT(u) FROM User u " +
                        "WHERE u.role != 'ROLE_CITIZEN' " +
                        "AND (:search IS NULL OR :search = '' OR " +
                        "     LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "     LOWER(u.department) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "     LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "     LOWER(u.empNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> findEmployeesWithProfilePageable(@Param("search") String search, Pageable pageable);
}
