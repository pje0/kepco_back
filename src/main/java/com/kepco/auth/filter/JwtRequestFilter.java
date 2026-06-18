package com.kepco.auth.filter;

import com.kepco.auth.provider.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    
    @Override
    protected boolean shouldNotFilter(jakarta.servlet.http.HttpServletRequest request) throws jakarta.servlet.ServletException {
        String path = request.getRequestURI();
        
        // 리액트가 찌르는 가입 주소 규격 매핑 (CORS Preflight인 OPTIONS 요청과 /api/register 패스를 예외 처리)
        return path.equals("/api/register") || path.equals("/register") || request.getMethod().equals("OPTIONS");
    }
    
    // 리액트에서 오는 모든 API 요청마다 딱 한 번씩 실행되는 필터 검사 로직
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. HTTP 요청 헤더에서 "Authorization" 값 추출
        String token = resolveToken(request);

        // 2. 토큰이 존재하고, JwtTokenProvider를 통해 검증했을 때 유효하다면 인증 처리
        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            // 토큰 내부의 유저 정보와 권한을 꺼내 시큐리티 인증 객체로 만듦
            Authentication authentication = jwtTokenProvider.getAuthentication(token);
            
            // 🎯 [여기 삽입!] 현재 시큐리티 메모리에 최종적으로 어떤 권한 문자열이 들어가는지 눈으로 확인하는 로그
            log.info("🚨 [인가 진단 콘솔] 현재 로그인 유저의 시큐리티 권한 목록: {}", authentication.getAuthorities());
            
            // 시큐리티 세션(Context)에 인증 성공 상태를 박아둠 (이 요청이 끝날 때까지만 유지)
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            log.info("@# JwtRequestFilter - JWT 토큰 인증 성공! (접속자: {})", authentication.getName());
        }

        // 3. 검사가 끝났으니 다음 필터 단계(또는 컨트롤러)로 요청을 넘김
        filterChain.doFilter(request, response);
    }

    // HTTP 요청 헤더에서 "Bearer [실제토큰문자열]" 패턴을 파싱해 주는 헬퍼 메서드
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // "Bearer " 글자(7칸)를 제외한 실제 JWT 값만 리턴
        }
        return null;
    }
}
