package com.kepco.auth.dto;

import com.kepco.auth.entity.User;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class AdminUserResponseDto {

    // =========================================================================
    // [1번 users 테이블 영역 - 모든 임직원 공통 기본 데이터]
    // =========================================================================
    private Long id;
    private String username;
    private String name;
    private String email;
    private String phone;
    private String role;        // WORKER, DISPATCHER, HR, ADMIN
    private LocalDate hiredAt;  // 모든 임직원 필수 반영

    // =========================================================================
    // [2번 recovery_worker 테이블 영역 - 현장직(WORKER) 경험이 있는 사원 데이터]
    // =========================================================================
    private String empNumber;         // 사번
    private String department;        // 소속 부서
    private String assignedDistrict;  // 담당 구역 (OpenAI 지역 매칭용)
    private String certificate;       // 자격증 (OpenAI 자격증 분석용)
    private String grade;             // 숙련도 (OpenAI 숙련도 분석용)
    private String workStatus;        // 💡 엔티티 스펙 반영: AVAILABLE, DISPATCHED, UNAVAILABLE

    /**
     * JPA User 엔티티 객체를 받아 화면단에 맞는 Response DTO로 유기적 변환
     * @param user 조회된 부모 엔티티 객체
     */
    public AdminUserResponseDto(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.name = user.getName();
        this.email = user.getEmail();
        this.phone = user.getPhone();
        this.role = user.getRole();
        this.hiredAt = user.getHiredAt();
        this.empNumber = user.getEmpNumber();
        this.department = user.getDepartment();

        // 💡 비즈니스 규칙 및 엔티티 Getter 명칭 정밀 반영
        if (user.getRecoveryWorker() != null) {
            this.assignedDistrict = user.getRecoveryWorker().getAssignedDistrict();
            this.certificate = user.getRecoveryWorker().getCertificate();
            this.grade = user.getRecoveryWorker().getGrade();
            this.workStatus = user.getRecoveryWorker().getWorkStatus(); // ⭕ getWorkStatus()로 정확히 호출
        } else {
            // 자식 프로필이 없는 순수 내근직(ADMIN, HR, DISPATCHER) 상태일 때의 방어용 기본값 세팅
            this.assignedDistrict = "";
            this.certificate = "";
            this.grade = "";
            this.workStatus = "UNAVAILABLE"; // 내근직 상태이므로 현장 미가동 기본 고정
        }
    }
}
