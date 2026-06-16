package com.kepco.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kepco.auth.dto.AdminUserRegisterDto;
import com.kepco.auth.dto.AdminUserUpdateRequestDto;
import com.kepco.auth.dto.RegisterRequestDto;
import com.kepco.auth.dto.UserUpdateRequestDto;
import com.kepco.auth.entity.User; // 💡 주의: 뒤이어 엔티티도 스키마에 맞게 뜯어고칠 예정입니다.
import com.kepco.auth.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * [대민 전용] 1. 민원인 회원가입 처리
     * - 규칙: 일반 민원인은 1번 users 테이블에만 저장되며, 권한은 무조건 'CITIZEN'으로 고정됩니다.
     */
    @Transactional
    public void register(RegisterRequestDto registerRequest) {
        log.info("@# AuthService - 민원인 회원가입 진행 중: {}", registerRequest.getLoginId());

        if (userRepository.findByLoginId(registerRequest.getLoginId()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }

        User user = User.builder()
                .loginId(registerRequest.getLoginId())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .name(registerRequest.getName())
                .email(registerRequest.getEmail())
                .phone(registerRequest.getPhone())
                .role("CITIZEN") // 💡 일반 민원인 권한 강제 부여
                .build();

        userRepository.save(user);
        log.info("@# 민원인 가입 DB 저장 완료: {}", user.getLoginId());
    }

    /**
     * [대민 전용] 2. 민원인 본인 정보 수정 (비번, 전번, 메일)
     */
    @Transactional
    public void updateUserInfo(String loginId, UserUpdateRequestDto updateRequest) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 값이 들어온 항목만 안전하게 엔티티 내부 메서드로 변경 (JPA 변경 감지 작동)
        if (updateRequest.getPassword() != null && !updateRequest.getPassword().isEmpty()) {
            user.changePassword(passwordEncoder.encode(updateRequest.getPassword())); 
        }
        if (updateRequest.getEmail() != null) {
            user.changeEmail(updateRequest.getEmail()); 
        }
        if (updateRequest.getPhone() != null) {
            user.changePhone(updateRequest.getPhone()); 
        }
    }

    /**
     * [대민 전용] 3. 민원인 본인 회원 탈퇴
     */
    @Transactional
    public void deleteUser(String loginId) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        userRepository.delete(user);
    }

    /* =========================================================================
     *  인사 관리 실무 전용 영역 (ROLE_HR, ROLE_ADMIN 대행 처리 엔진)
     * ========================================================================= */

    /**
     * 4. [인사팀 전용] 신입 사원 계정 및 OpenAI용 직무 정보 연쇄 생성 (핵심)
     * - 'WORKER' 권한으로 생성 시 1번 테이블 저장 후 발생한 PK를 들고 2번 복구팀 테이블까지 동시에 연쇄 인서트합니다.
     */
    @Transactional
    public void createEmployee(AdminUserRegisterDto employeeDto) {
        log.info("@# AuthService - 인사팀에 의한 사원 등록 진행 중: {}", employeeDto.getLoginId());

        if (userRepository.findByLoginId(employeeDto.getLoginId()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 사원 아이디입니다.");
        }

        // 4-1. 1번 users 테이블 객체 빌드 및 저장
        User user = User.builder()
                .loginId(employeeDto.getLoginId())
                .password(passwordEncoder.encode(employeeDto.getPassword()))
                .name(employeeDto.getName())
                .email(employeeDto.getEmail())
                .phone(employeeDto.getPhone())
                .role(employeeDto.getRole().toUpperCase()) // 스키마 대문자 표준 반영 (WORKER, HR 등)
                .build();

        // 💡 중요: save()를 실행하면 영속성 컨텍스트에 의해 자동으로 id(자동증가 PK)가 user 객체에 장착됩니다.
        userRepository.save(user);

        // 4-2. 입력된 권한이 'WORKER'(현장직)일 경우에만 2번 recovery_worker 테이블에 연쇄 데이터 세팅
        if ("WORKER".equalsIgnoreCase(user.getRole())) {
            // 뒤이어 만들 관계 매핑 메서드를 통해 엔티티 내부에 인사정보 강제 주입
            user.createRecoveryWorkerProfile(
                    employeeDto.getEmpNumber(),
                    employeeDto.getDepartment(),
                    employeeDto.getAssignedDistrict(),
                    employeeDto.getCertificate(),
                    employeeDto.getGrade()
            );
            log.info("@# 현장 복구팀 확장 인사 정보(OpenAI 타겟) 연쇄 저장 완료");
        }
        log.info("@# 최종 사원 계정 생성 성공: {}", user.getLoginId());
    }

    /**
     * 5. [인사팀 전용] 사원 기본 신상 및 OpenAI 관제용 자격증/스펙 수정 (변경 감지 활용)
     */
    @Transactional
    public void updateEmployeeInfoByAdmin(Long id, AdminUserUpdateRequestDto updateRequest) {
        log.info("@# AuthService - 사원 정보 및 AI 스펙 수정 시도 (사원 고유번호: {})", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사원 정보입니다."));

        // 1번 테이블 기본 신상 변경
        if (updateRequest.getName() != null) user.changeName(updateRequest.getName());
        if (updateRequest.getEmail() != null) user.changeEmail(updateRequest.getEmail());
        if (updateRequest.getPhone() != null) user.changePhone(updateRequest.getPhone());

        // 2번 테이블 OpenAI용 자격증 및 직급 스펙 실시간 변경 연동
        if ("WORKER".equalsIgnoreCase(user.getRole()) && user.getRecoveryWorker() != null) {
            user.getRecoveryWorker().updateWorkerSpecs(
                    updateRequest.getDepartment(),
                    updateRequest.getAssignedDistrict(),
                    updateRequest.getCertificate(),
                    updateRequest.getGrade()
            );
            log.info("@# 사원의 OpenAI 추천 데이터(자격증/직급/구역)가 실시간 업데이트 되었습니다.");
        }
    }

    /**
     * 6. [인사팀/관리자 전용] 사원 직무 권한(Role) 변경 처리
     */
    @Transactional
    public void updateUserRole(Long id, AdminUserUpdateRequestDto roleRequest) { // ⭕ 통합 DTO로 매핑 변경
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사원 정보입니다."));

        // 💡 통합 그릇에서 새 권한(role)을 꺼내 대문자로 변환 후 세팅합니다.
        if (roleRequest.getRole() != null) {
            user.changeRole(roleRequest.getRole().toUpperCase());
            log.info("@# 사원 직무 권한 변경 완료: {} -> {}", user.getLoginId(), user.getRole());
        }
    }


    /**
     * 7. [인사팀 전용] 사원 퇴사 처리 (계정 및 확장 데이터 통째로 영구 삭제)
     * - 💡 컨트롤러에서 호출하던 deleteEmployeeByAdmin 에러 파괴 완료!
     * - 스키마의 ON DELETE CASCADE 설정 덕분에 1번을 지우면 2번 복구팀 테이블은 자동으로 폭파됩니다.
     */
    @Transactional
    public void deleteEmployeeByAdmin(Long id) {
        log.info("@# AuthService - 사원 퇴사(영구 삭제) 처리 진행 중 (사원 고유번호: {})", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사원 정보입니다."));

        userRepository.delete(user);
        log.info("@# 사원 데이터 전체 영구 삭제 완료 (퇴사 발령 마감)");
    }
}
