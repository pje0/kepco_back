package com.kepco.auth.contants;

public final class SecurityConstants {
    
    // 외부에서 new SecurityConstants()로 객체를 생성하는 것을 원천 차단
    private SecurityConstants() {
        throw new IllegalStateException("Utility class");
    }

    public static final String TOKEN_HEADER = "Authorization"; // HTTP 헤더 키값
    public static final String TOKEN_PREFIX = "Bearer ";       // 토큰 접두사 (뒤에 공백 한 칸 필수!)
    public static final String TOKEN_TYPE = "JWT";             // 토큰 메타데이터 타입
    
    public static final String AUTH_LOGIN_URL = "/login";      // 스프링 시큐리티 로그인 엔드포인트
}
