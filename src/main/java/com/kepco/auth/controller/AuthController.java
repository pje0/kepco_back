package com.kepco.auth.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.kepco.auth.dto.AdminUserRegisterDto;       // 💡 사원 가입용 DTO 임포트
import com.kepco.auth.dto.AdminUserUpdateRequestDto; // 💡 사원 수정용 DTO 임포트
import com.kepco.auth.dto.AdminUserResponseDto;      // 💡 [추가] 사원 목록 응답용 DTO 임포트
import com.kepco.auth.dto.RegisterRequestDto;
import com.kepco.auth.dto.UserUpdateRequestDto;
import com.kepco.auth.repository.UserRepository;
import com.kepco.auth.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository; 

    /* =========================================================================
     *  1. 대민 민원인 전용 기능 (ROLE_CITIZEN 전용 대문)
     * ========================================================================= */

    /**
     * 1-1. 민원인 회원가입 API
     * - URL: POST /api/auth/register
     * - 규칙: 누구나 접근 가능, 서비스 단에서 무조건 CITIZEN 권한 부여
     */
    @PostMapping("/api/auth/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequestDto registerRequest) {
        try {
            authService.register(registerRequest);
            return ResponseEntity.ok(Map.of("message", "한전 MIS 시스템에 회원가입이 완료되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 1-2. 민원인 본인 정보 수정 API (보안 표준 - 대안 A)
     * - URL: PUT /api/user/me
     */
    @PutMapping("/api/user/me")
    public ResponseEntity<?> update(@RequestBody UserUpdateRequestDto updateRequest,
                                    @AuthenticationPrincipal User principal) {
        try {
            authService.updateUserInfo(principal.getUsername(), updateRequest);
            return ResponseEntity.ok(Map.of("message", "회원 정보가 성공적으로 수정되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 1-3. 민원인 회원 탈퇴 API (보안 표준 - 대안 A)
     * - URL: DELETE /api/user/me
     */
    @DeleteMapping("/api/user/me")
    public ResponseEntity<?> withdraw(@AuthenticationPrincipal User principal) {
        try {
            authService.deleteUser(principal.getUsername());
            return ResponseEntity.ok(Map.of("message", "회원 탈퇴가 정상적으로 처리되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /* =========================================================================
     *  2. 인사 관리 실무자 전용 기능 (ROLE_HR, ROLE_ADMIN 전용 대문)
     * ========================================================================= */

    /**
     * 2-1. 신입 사원 계정 생성 API (인사팀 대행 등록 방식)
     * - URL: POST /api/hr/user
     * - 규칙: 사원의 기본 계정 정보와 OpenAI 분석용 직무 데이터를 처음부터 일괄 생성
     */
    @PostMapping("/api/hr/user")
    public ResponseEntity<?> createEmployee(@Valid @RequestBody AdminUserRegisterDto employeeDto) {
        try {
            authService.createEmployee(employeeDto);
            return ResponseEntity.ok(Map.of("message", "새로운 사원 정보가 시스템에 성공적으로 등록되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 2-2. 사원 기본 신상 및 OpenAI 분석 스펙(자격증 등) 수정 API
     * - URL: PUT /api/hr/user/{id}
     */
    @PutMapping("/api/hr/user/{id}")
    public ResponseEntity<?> updateEmployeeInfo(@PathVariable("id") Long id,
                                                @RequestBody AdminUserUpdateRequestDto updateRequest) {
        try {
            authService.updateEmployeeInfoByAdmin(id, updateRequest);
            return ResponseEntity.ok(Map.of("message", "사원의 신상 및 직무 스펙 정보가 성공적으로 수정되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 2-3. 사원 직무 권한(부서 및 Role) 변경 API
     * - URL: PUT /api/hr/user/{id}/role
     */
    @PutMapping("/api/hr/user/{id}/role")
    public ResponseEntity<?> changeUserRole(@PathVariable("id") Long id,
                                            @RequestBody AdminUserUpdateRequestDto roleRequest) {
        try {
            authService.updateUserRole(id, roleRequest);
            return ResponseEntity.ok(Map.of("message", "사원의 직무 권한이 성공적으로 변경되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 2-4. 사원 퇴사 처리 API (삭제)
     * - URL: DELETE /api/hr/user/{id}
     */
    @DeleteMapping("/api/hr/user/{id}")
    public ResponseEntity<?> fireEmployee(@PathVariable("id") Long id) {
        try {
            authService.deleteEmployeeByAdmin(id);
            return ResponseEntity.ok(Map.of("message", "해당 사원의 퇴사 처리가 완료되어 계정이 삭제되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 2-5. [신규 추가] 임직원 명부 전체 조회 API
     * - URL 규격: GET /api/hr/users
     * - 규칙: SecurityConfig의 /api/hr/** 정책에 연동되어 자동으로 HR, ADMIN만 접근 허용
     */
    @GetMapping("/api/hr/users")
    public ResponseEntity<?> getAllEmployeesList() {
        try {
            // 1. 서비스 비즈니스 엔진 호출하여 100% 무결성 DTO 리스트 수신
            List<AdminUserResponseDto> employees = authService.getAllEmployees();
            
            // 2. 리액트 레이어가 즉각 렌더링하도록 200 OK와 함께 JSON 배열 송출
            return ResponseEntity.ok(employees);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "임직원 명부를 불러오는 중 오류가 발생했습니다. " + e.getMessage()));
        }
    }

    /* =========================================================================
     *  3. 로그인 회원 공통 인증 기능 (내 정보 조회)
     * ========================================================================= */

    /**
     * 3-1. 현재 로그인한 사용자의 최신 상세 정보 조회 API
     * - URL: GET /api/auth/me
     * - 프론트엔드에서 새로고침 하거나 로그인 직후 세션 유지를 위해 토큰을 검증하고 데이터를 가져오는 핵심 창구입니다.
     */
    @GetMapping("/api/auth/me")
    public ResponseEntity<?> getCurrentUserInfo(@AuthenticationPrincipal User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("message", "인증 정보가 만료되었거나 올바르지 않습니다."));
        }
        
        try {
            String username = principal.getUsername();
            
            // 🎯 DB에서 실제 계정 실시간 조회
            com.kepco.auth.entity.User dbUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계정입니다."));

            String rawRole = dbUser.getRole() != null ? dbUser.getRole() : "ROLE_CITIZEN";
            String finalRole = rawRole.toUpperCase(); // 무조건 ROLE_WORKER, ROLE_ADMIN 형태로 고정

            Map<String, Object> userData = Map.of(
                "username", username,
                "role", finalRole,          // ⭕ 프론트엔드 표준 규격인 "ROLE_WORKER", "ROLE_CITIZEN" 등으로 반환
                "name", dbUser.getName(),   // DB에 기록된 진짜 실명 반환
                "phone", dbUser.getPhone(),  
                "email", dbUser.getEmail(),
                "department", dbUser.getDepartment()
            );

            return ResponseEntity.ok(userData);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "사용자 정보 조회 실패: " + e.getMessage()));
        }
    }
}
