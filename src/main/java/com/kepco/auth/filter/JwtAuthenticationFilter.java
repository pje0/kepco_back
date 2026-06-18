package com.kepco.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kepco.auth.dto.LoginRequestDto; // 아래에 추가로 만들 DTO 패키지 경로
import com.kepco.auth.provider.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

@Slf4j
public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        
        // 💡 중요: 리액트가 프록시를 통해 요청하는 실제 API 엔드포인트 주소인 /api/auth/login 으로 명시적 변경합니다!
        setFilterProcessesUrl("/api/auth/login"); 
    }

    // 1. 리액트에서 보낸 로그인 요청(JSON 형태)을 가로채서 인증 시도
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) 
            throws AuthenticationException {
        try {
            log.info("@# JwtAuthenticationFilter - 로그인 요청 감지 (/login)");
            
            // 리액트가 보낸 JSON { "username": "...", "password": "..." } 데이터를 DTO로 변환
            ObjectMapper objectMapper = new ObjectMapper();
            LoginRequestDto loginRequest = objectMapper.readValue(request.getInputStream(), LoginRequestDto.class);

            // 시큐리티 인증용 토큰 객체 생성
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword());

            // AuthenticationManager에게 위임하여 실제 비밀번호 검증 진행 (CustomUserDetailsService가 여기서 실행됨)
            return authenticationManager.authenticate(authenticationToken);

        } catch (IOException e) {
            log.error("@# 로그인 요청 데이터 파싱 실패: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // 2. 인증 성공 시 실행 (비밀번호 일치 -> JWT 토큰 발급 후 응답 헤더에 세팅)
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, 
                                            FilterChain chain, Authentication authResult) 
            throws IOException, ServletException {
        log.info("@# 로그인 성공! JWT 토큰 발급 시작 (유저: {})", authResult.getName());

        // JWT 토큰 생성
        String token = jwtTokenProvider.createToken(authResult);

        // 리액트가 읽을 수 있도록 응답 헤더에 Bearer 토큰으로 추가
        response.addHeader("Authorization", "Bearer " + token);
        
        // 응답 바디(JSON)로도 토큰을 내려주고 싶다면 아래 코드 활성화
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\": \"로그인 성공!\", \"token\": \"" + token + "\"}");
    }

    // 3. 인증 실패 시 실행 (비밀번호 틀림 등)
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, 
                                              AuthenticationException failed) 
            throws IOException, ServletException {
        log.warn("@# 로그인 실패: {}", failed.getMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\": \"아이디 또는 비밀번호가 올바르지 않습니다.\"}");
    }
    
}
