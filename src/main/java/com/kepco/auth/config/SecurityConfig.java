package com.kepco.auth.config;

import com.kepco.auth.filter.JwtAuthenticationFilter;
import com.kepco.auth.filter.JwtRequestFilter;
import com.kepco.auth.provider.JwtTokenProvider;
import com.kepco.auth.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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

import java.util.List;

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
                
             // 🔐 최종 DB 스키마 규격 반영 및 이원화 인가 제어 (프론트엔드 /api/ 주소 완벽 튜닝)
                .authorizeHttpRequests(auth -> auth
                        // 1. 로그인, 로그아웃 및 민원인 가입 창구 전면 허용 (개나소나 프리패스 구역)
                        .requestMatchers(
                            "/", "/login", "/register", 
                            "/api/auth/login", "/api/auth/logout", "/api/auth/register", "/api/register",
                            "/api/notices", "/api/notices/**", "/notices", "/notices/**",
                            "/error"
                        ).permitAll()
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        
                        // 2. 민원인 본인 정보 수정/탈퇴 전용 마이페이지
//                        .requestMatchers("/api/user/me", "/user/me").hasRole("CITIZEN")
                        // 본인 정보 수정 기능은 전 직원 공용임 - 박정은
                        .requestMatchers("/api/user/me", "/user/me").authenticated()
                        
                        // 3. 민원인 전용 비즈니스 처리 영역
                        .requestMatchers("/api/citizen/**", "/citizen/**").hasAnyRole("CITIZEN", "ADMIN")
                        
                        // 4. [인사팀 전용 사원 관리 경로]
                        .requestMatchers("/api/hr/**", "/hr/**").hasAnyRole("HR", "ADMIN")
                        
                        //⚡ [교정] 신규 하위 경로 패턴을 명시적으로 독립 선언하여 와일드카드 오매칭 우회
                        .requestMatchers("/api/dispatch/history").hasAnyRole("DISPATCHER", "ADMIN")

                        // 5. (기존 소스 보존) [파견관리팀 전용 관제 경로]
                        .requestMatchers("/api/dispatch/**", "/dispatch/**").hasAnyRole("DISPATCHER", "ADMIN")
                        
                        // 6. [현장근무자 전용 복구 경로] 순수 WORKER만 단독 접근 허용
                        .requestMatchers("/api/worker/**", "/worker/**").hasRole("WORKER")
                        
                        // 7. 최고 관리자 전용 서버 마스터 시스템 통제 경로
                        .requestMatchers("/api/admin/system/**", "/admin/system/**").hasRole("ADMIN")
                        
                        // 자료실: 목록·다운로드 (전 직원 공용) - 박정은 추가
                        .requestMatchers(HttpMethod.POST, "/api/archive").hasRole("ADMIN")
                        .requestMatchers("/api/archive", "/api/archive/**").permitAll()
                        
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

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
