package com.kepco.auth.config;

import com.kepco.auth.filter.JwtAuthenticationFilter;
import com.kepco.auth.filter.JwtRequestFilter;
import com.kepco.auth.provider.JwtTokenProvider;
import com.kepco.auth.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// ⭕ reactive 제거된 올바른 MVC용 PathRequest 경로
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    // 1. 시큐리티 인증 매니저 빈 등록
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {
        log.info("@# SecurityConfig - authenticationManager() 등록");
        return configuration.getAuthenticationManager();
    }

    // 2. 메인 시큐리티 필터 체인 제어 및 필터 조립
    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            AuthenticationManager authenticationManager) throws Exception {
        log.info("@# SecurityConfig - filterChain() 조립 시작");

        // 로그인 인증 필터 생성
        JwtAuthenticationFilter jwtAuthenticationFilter =
                new JwtAuthenticationFilter(authenticationManager, jwtTokenProvider);

        http
                // 기본 세션 보안 기능 비활성화 (JWT 정책)
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                // 리액트 연동을 위한 CORS 정책 허용
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 세션 Stateless 모드 설정
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 유저 검증 서비스 바인딩
                .userDetailsService(customUserDetailsService)

                // 접근 제어 권한 인가 설정
                .authorizeHttpRequests(auth -> auth
                        // 메인화면, 로그인, 회원가입 관련 주소 전면 허용
                        .requestMatchers("/", "/login", "/api/auth/**").permitAll()
                        // 정적 리소스 통과
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        // 관리자 기능 통제
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // 그 외 모든 요청은 JWT 토큰 필수
                        .anyRequest().authenticated()
                )

                // [필터 조립 1] 로그인 요청 시 동작할 인증 필터 배치
                .addFilterAt(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                // [필터 조립 2] 오타 전면 수정된 토큰 검증 인가 필터 정상 배치
                .addFilterBefore(
                        new JwtRequestFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    // 3. 비밀번호 암호화 빈 등록
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 4. 리액트 연동용 CORS 설정
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        
        config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173"
        ));
        
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        
        return source;
    }
}
