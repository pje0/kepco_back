package com.kepco.auth.provider;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long expirationTime;

    // application.properties에서 설정값을 주입받아 빌드
    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expirationTime) {
        // jjwt 0.12.x 최신 표준 키 생성 방식
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationTime = expirationTime;
    }

    // 1. 인증에 성공한 사용자의 정보(Authentication)를 받아 JWT 토큰 생성
    public String createToken(Authentication authentication) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        Date now = new Date();
        Date validity = new Date(now.getTime() + expirationTime);

        log.info("@# JwtTokenProvider - 토큰 생성 시작 (유저: {})", authentication.getName());

        return Jwts.builder()
                .subject(authentication.getName()) // ID 저장
                .claim("auth", authorities)        // 권한 등급 저장 (ROLE_USER 등)
                .issuedAt(now)
                .expiration(validity)
                .signWith(secretKey)               // 서명 암호화 키 세팅
                .compact();
    }

    // 2. HTTP 헤더에서 넘어온 JWT 토큰을 복호화하여 시큐리티 인증 객체(Authentication) 추출
    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey) // jjwt 0.12.x 검증 방식
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get("auth").toString().split(","))
                        .filter(auth -> !auth.trim().isEmpty())
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        UserDetails principal = new User(claims.getSubject(), "", authorities);
        
        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    // 3. 토큰이 변조되었거나 유효기간이 만료되었는지 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("@# 유효하지 않거나 만료된 JWT 토큰입니다: {}", e.getMessage());
        }
        return false;
    }
}
