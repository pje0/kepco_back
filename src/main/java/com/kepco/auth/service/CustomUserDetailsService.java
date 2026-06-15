package com.kepco.auth.service;

import com.kepco.auth.entity.User;
import com.kepco.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("@# CustomUserDetailsService - loadUserByUsername() 실행: {}", username);

        // 1. DB에서 사용자 아이디로 조회 (없으면 시큐리티 예외 발생)
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("@# 사용자 찾을 수 없음: {}", username);
                    return new UsernameNotFoundException("존재하지 않는 사용자입니다: " + username);
                });

        // 2. 스프링 시큐리티 전용 UserDetails 객체로 변환하여 리턴
        // 사용자의 실제 비밀번호(암호화된 상태)와 권한(ROLE_USER 등)을 시큐리티에게 넘겨줍니다.
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole()))
        );
    }
}