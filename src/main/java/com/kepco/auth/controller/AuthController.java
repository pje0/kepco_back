package com.kepco.auth.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.kepco.auth.dto.AdminUserRegisterDto;       // 💡 사원 가입용 DTO 임포트
import com.kepco.auth.dto.AdminUserUpdateRequestDto; // 💡 사원 수정용 DTO 임포트
import com.kepco.auth.dto.RegisterRequestDto;
import com.kepco.auth.dto.UserUpdateRequestDto;
import com.kepco.auth.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /* =========================================================================
     *  1. 대민 민원인 전용 기능 (ROLE_CITIZEN 전용 대문)
     * ========================================================================= */

    /**
     * 1-1. 민원인 회원가입 API
     * - URL: POST /register
     * - 규칙: 누구나 접근 가능, 서비스 단에서 무조건 CITIZEN 권한 부여
     */
    @PostMapping("/register")
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
     * - URL: PUT /user/me
     */
    @PutMapping("/user/me")
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
     * - URL: DELETE /user/me
     */
    @DeleteMapping("/user/me")
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
     * - URL: POST /hr/user
     * - 규칙: 사원의 기본 계정 정보와 OpenAI 분석용 직무 데이터를 처음부터 일괄 생성
     */
    @PostMapping("/hr/user")
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
     * - URL: PUT /hr/user/{id}
     */
    @PutMapping("/hr/user/{id}")
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
     * - URL: PUT /hr/user/{id}/role
     */
    @PutMapping("/hr/user/{id}/role")
    public ResponseEntity<?> changeUserRole(@PathVariable("id") Long id,
                                            @RequestBody AdminUserUpdateRequestDto roleRequest) { // ⭕ 통합 DTO로 매핑 변경
        try {
            authService.updateUserRole(id, roleRequest);
            return ResponseEntity.ok(Map.of("message", "사원의 직무 권한이 성공적으로 변경되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }


    /**
     * 2-4. 사원 퇴사 처리 API (삭제)
     * - URL: DELETE /hr/user/{id}
     */
    @DeleteMapping("/hr/user/{id}")
    public ResponseEntity<?> fireEmployee(@PathVariable("id") Long id) {
        try {
            authService.deleteEmployeeByAdmin(id);
            return ResponseEntity.ok(Map.of("message", "해당 사원의 퇴사 처리가 완료되어 계정이 삭제되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
