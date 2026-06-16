package com.kepco.auth.repository;

import com.kepco.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // ⭕ 기존 findByUsername을 지우고, 변경된 컬럼명인 loginId에 맞추어 쿼리 메서드 교정 완료!
    Optional<User> findByLoginId(String loginId);
}
