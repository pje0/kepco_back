package com.kepco.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

import com.kepco.auth.dto.AdminUserRegisterDto;
import com.kepco.auth.dto.AdminUserResponseDto;
import com.kepco.auth.dto.AdminUserUpdateRequestDto;
import com.kepco.auth.dto.RegisterRequestDto;
import com.kepco.auth.dto.UserUpdateRequestDto;
import com.kepco.auth.entity.User;
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
                .role("ROLE_CITIZEN")
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
     * 4. [인사팀 전용] 신입 사원 계정 및 OpenAI용 직무 정보 연쇄 생성
     * - 'WORKER' 권한으로 생성 시 1번 테이블 저장 후 발생한 PK를 들고 2번 복구팀 테이블까지 동시에 연쇄 인서트합니다.
     */
    @Transactional
    public void createEmployee(AdminUserRegisterDto employeeDto) {
        log.info("@# AuthService - 인사팀에 의한 사원 등록 진행 중: {}", employeeDto.getLoginId());

        if (userRepository.findByLoginId(employeeDto.getLoginId()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 사원 아이디입니다.");
        }

        // 💡 [교정]: 인사팀이 던진 순수 단어에 'ROLE_'를 강제 결합하여 DB 무결성 확보
        String formattedRole = "ROLE_" + employeeDto.getRole().toUpperCase();

        User user = User.builder()
                .loginId(employeeDto.getLoginId())
                .password(passwordEncoder.encode(employeeDto.getPassword()))
                .name(employeeDto.getName())
                .email(employeeDto.getEmail())
                .phone(employeeDto.getPhone())
                .role(formattedRole) // ⬅️ 보정된 'ROLE_XXXX' 주입
                .build();

        userRepository.save(user);

        // 💡 [교정]: 데이터가 'ROLE_WORKER'로 들어가므로 비교문도 완벽하게 싱크 조율
        if ("ROLE_WORKER".equalsIgnoreCase(user.getRole())) {
            user.createRecoveryWorkerProfile(
                    employeeDto.getEmpNumber(),
                    employeeDto.getDepartment(),
                    employeeDto.getAssignedDistrict(),
                    employeeDto.getCertificate(),
                    employeeDto.getGrade()
            );
            log.info("@# 현장 복구팀 확장 인사 정보(OpenAI 타겟) 연쇄 저장 완료");
        }
    }

    /**
     * 5. [인사팀 전용] 사원 기본 신상 및 OpenAI 관제용 자격증/스펙 수정
     */
    @Transactional
    public void updateEmployeeInfoByAdmin(Long id, AdminUserUpdateRequestDto updateRequest) {
        log.info("@# AuthService - 사원 정보 및 AI 스펙 수정 시도 (사원 고유번호: {})", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사원 정보입니다."));

        if (updateRequest.getName() != null) user.changeName(updateRequest.getName());
        if (updateRequest.getEmail() != null) user.changeEmail(updateRequest.getEmail());
        if (updateRequest.getPhone() != null) user.changePhone(updateRequest.getPhone());

        // 💡 [교정]: 2번 테이블 스펙 변경 트리거 조건도 'ROLE_WORKER'로 싱크 완료
        if ("ROLE_WORKER".equalsIgnoreCase(user.getRole()) && user.getRecoveryWorker() != null) {
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
     * 6. [인사팀 전용] 사원 직무 권한(Role) 변경 처리 
     */
    @Transactional
    public void updateUserRole(Long id, AdminUserUpdateRequestDto roleRequest) {
        log.info("@# AuthService - 사원 보직 변경 발령 시도 (사원 고유번호: {})", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사원 정보입니다."));

        if (roleRequest.getRole() != null) {
            String oldRole = user.getRole(); 
            // 💡 [교정]: 변경 요청 들어온 보직에도 'ROLE_'를 강제로 붙여서 연산
            String newRole = "ROLE_" + roleRequest.getRole().toUpperCase(); 
            
            // 1. 1번 테이블 권한 변경
            user.changeRole(newRole);

            // 💡 [교정]: 분기 규칙 검사 시에도 전부 'ROLE_WORKER' 기준으로 철저하게 방어
            // [분기 A] 타팀 -> 현장직(WORKER) 최초 보직 이동 시 (신규 생성)
            if ("ROLE_WORKER".equals(newRole) && user.getRecoveryWorker() == null) {
                user.createRecoveryWorkerProfile(
                        "EMP-" + user.getId() + "-" + System.currentTimeMillis() % 10000,
                        "미배정 부서", "대기 지역", "자격증 정보를 등록해 주세요", "JUNIOR"
                );
                log.info("@# 🔗 [인사 발령] 현장 복구팀 프로필이 자동으로 신규 인서트 되었습니다.");
            }
            
            // [분기 B] 기존 현장직(WORKER) -> 내근직(HR, DISPATCHER 등)으로 복귀 시 (출동 불가 잠금)
            else if (!"ROLE_WORKER".equals(newRole) && "ROLE_WORKER".equals(oldRole) && user.getRecoveryWorker() != null) {
                user.getRecoveryWorker().changeWorkStatus("UNAVAILABLE"); 
                log.info("@# 🔐 [인사 발령] 내근직 전환 감지: recovery_worker 데이터를 보존하고 '출동 불가(UNAVAILABLE)' 상태로 잠금 완료.");
            }
            
            // [분기 C] 예전에 WORKER였다가 내근직에 있던 사람이 다시 WORKER로 복귀 시 (잠금 해제 / 대기 상태 원복)
            else if ("ROLE_WORKER".equals(newRole) && user.getRecoveryWorker() != null) {
                user.getRecoveryWorker().changeWorkStatus("AVAILABLE"); 
                log.info("@# 🔓 [인사 발령] 현장직 재복귀 감지: 기존 이력 프로필을 재활용하여 '출동 가능(AVAILABLE)' 상태로 원복 완료.");
            }
        }
    }

    /**
     * 7. [인사팀 전용] 사원 퇴사 처리 (계정 및 확장 데이터 통째로 영구 삭제)
     */
    @Transactional
    public void deleteEmployeeByAdmin(Long id) {
        log.info("@# AuthService - 사원 퇴사 처리 진행 중 (사원 고유번호: {})", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사원 정보입니다."));

        userRepository.delete(user);
        log.info("@# 사원 데이터 전체 영구 삭제 완료 (퇴사 발령 마감)");
    }
    /**
     * 8. [인사팀 전용] 임직원 전체 목록 조회 (JPA 영속성 최적화 및 DTO 통합 파싱)
     * - 'ROLE_CITIZEN'이 아닌 모든 역할을 임직원으로 취급하여 신규 보직 추가에 유연하게 대응합니다.
     * - 부모(User)와 자식(RecoveryWorker) 테이블을 한방에 긁어와 N+1 성능 저하를 차단합니다.
     */
    @Transactional(readOnly = true) // 💡 대량 조회 성능 최적화 보장
    public List<AdminUserResponseDto> getAllEmployees() {
        log.info("@# AuthService - 인사팀/관리자에 의한 임직원 명부 전체 조회 실행");

        // 1. Repository의 Fetch Join 쿼리를 호출하여 엔티티 리스트 획득
        List<User> employees = userRepository.findAllEmployeesWithProfile();

        // 2. Java Stream API를 활용하여 안전하게 Response DTO 리스트로 일괄 래핑 및 변환
        return employees.stream()
                .map(AdminUserResponseDto::new)
                .collect(Collectors.toList());
    }
}
