package com.kepco.auth.service;

import com.kepco.auth.dto.RegisterRequestDto;
import com.kepco.auth.entity.User;
import com.kepco.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void register(RegisterRequestDto registerRequest) {
        log.info("@# AuthService - 회원가입 진행 중: {}", registerRequest.getUsername());

        // 1. 아이디 중복 체크
        if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }

        // 2. 엔티티 생성 및 비밀번호 암호화(BCrypt) 후 세팅
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword())); // 시큐리티 암호화 연동
        user.setEmail(registerRequest.getEmail());
        user.setRole("ROLE_USER"); // 한전 기본 직원 권한 부여

        // 3. DB 저장
        userRepository.save(user);
        log.info("@# 회원가입 DB 저장 완료: {}", user.getUsername());
    }
}
