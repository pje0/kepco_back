package com.kepco.auth.config;

import java.util.List;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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

import com.kepco.auth.filter.JwtAuthenticationFilter;
import com.kepco.auth.filter.JwtRequestFilter;
import com.kepco.auth.provider.JwtTokenProvider;
import com.kepco.auth.service.CustomUserDetailsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(authenticationManager, jwtTokenProvider);

        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .userDetailsService(customUserDetailsService)
                
                .authorizeHttpRequests(auth -> auth
                        // 1. 공개 허용 경로 설정
                        .requestMatchers(
                            "/", "/login", "/register", "/reports", "/reports/**",
                            "/api/auth/login", "/api/auth/logout", "/api/auth/register", "/api/register"
                        ).permitAll()
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        
                        // 2. 공용 마이페이지
                        .requestMatchers("/api/user/me", "/user/me").authenticated()
                        
                        // 3. 민원인 비즈니스 영역
                        .requestMatchers("/api/citizen/**", "/citizen/**").hasAnyRole("CITIZEN", "ADMIN")
                        
                        // ⚡ [근본 해결 - 민원 주소 인가 독립 매핑 신설]
                        // - 403 Forbidden 원천 분쇄를 위해 관제사 및 마스터 관리자 등급 전용으로 개통
                        .requestMatchers("/api/complaint", "/api/complaint/**").hasAnyRole("DISPATCHER", "ADMIN")
                        
                        // 4. 인사팀 전용 관리 영역
                        .requestMatchers("/api/hr/**", "/hr/**").hasAnyRole("HR", "ADMIN")
                        
                        // 5. 파견관리팀 전용 관제 영역
                        .requestMatchers("/api/dispatch/history").hasAnyRole("DISPATCHER", "ADMIN")
                        .requestMatchers("/api/dispatch/**", "/dispatch/**").hasAnyRole("DISPATCHER", "ADMIN")
                        
                        // 6. 현장근무자 영역
                        .requestMatchers("/api/worker/**", "/worker/**").hasRole("WORKER")
                        
                        // 7. 최고 관리자 영역
                        .requestMatchers("/api/admin/system/**", "/admin/system/**").hasRole("ADMIN")
                        
                        .anyRequest().authenticated()
                )
                .addFilterAt(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new JwtRequestFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS(Cross-Origin Resource Sharing) 인프라 매핑 설정
     * 💡 [로그 주입]: 브라우저 OPTIONS preflight 패킷 연산 및 403 차단 추적을 위한 디버깅용 커스텀 소스 가동
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        
        // 프론트엔드 Vite 개발 서버 포트(5173) 및 외부 통신 축 명시적 전면 수용
        config.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173", "http://127.0.0.1:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);
        
        // 🚨 [ROLE_ROLE_ 중복 검증 콘솔 로그 주입]: Spring Security 인가 가드 가동 상태 실시간 출력
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🛰️ [KEPCO MIS Security 인프라 디버깅]: CORS 필터 및 OPTIONS 가드 가동 시작");
        log.info("🚨 [ROLE_ROLE_ 검증 가이드]: 만약 로그인 시 토큰의 Authority 가 'ROLE_ROLE_ADMIN' 등으로");
        log.info("   중복 결합되어 매싱 중이라면, JwtRequestFilter 단에서 접두사 정제가 누락된 상태입니다.");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
