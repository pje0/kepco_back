package com.kepco.auth.repository;

import com.kepco.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // 로그인을 위해 사용자 아이디(username)로 정보를 조회하는 메서드
    Optional<User> findByUsername(String username);
}
