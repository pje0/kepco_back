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

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expirationTime) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationTime = expirationTime;
    }

    public String createToken(Authentication authentication) {
        // 💡 [핵심 교정]: 시큐리티 인증 객체 내부 권한 문자열을 추출할 때, 
        // 이미 ROLE_ROLE_ 구조로 오염되어 있다면 깨끗하게 걷어내어 오직 단 한 번만 감싸진 온전한 규격("ROLE_HR")으로 토큰에 각인합니다.
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(role -> {
                    String cleanRole = role.trim();
                    if (cleanRole.startsWith("ROLE_ROLE_")) {
                        cleanRole = cleanRole.substring(5); // "ROLE_ROLE_HR" -> "ROLE_HR"로 강제 보정
                    }
                    return cleanRole;
                })
                .collect(Collectors.joining(","));

        Date now = new Date();
        Date validity = new Date(now.getTime() + expirationTime);

        return Jwts.builder()
                .subject(authentication.getName()) 
                .claim("auth", authorities)        
                .issuedAt(now)
                .expiration(validity)
                .signWith(secretKey)               
                .compact();
    }

    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey) 
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get("auth").toString().split(","))
                        .filter(auth -> !auth.trim().isEmpty())
                        .map(auth -> {
                            String role = auth.trim();
                            // 💡 핵심 교정: 이미 토큰에 ROLE_이 붙어와서 시큐리티가 ROLE_ROLE_HR로 중복 인식하는 현상을 방지합니다.
                            if (role.startsWith("ROLE_")) {
                                role = role.substring(5); // "ROLE_" 5글자를 잘라내어 "HR" 순수 단어만 시큐리티에 넘겨줍니다.
                            }
                            return new SimpleGrantedAuthority("ROLE_" + role); // 시큐리티 규격에 맞춰 단 한 번만 온전하게 빌드
                        })
                        .collect(Collectors.toList());

        UserDetails principal = new User(claims.getSubject(), "", authorities);
        
        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

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
