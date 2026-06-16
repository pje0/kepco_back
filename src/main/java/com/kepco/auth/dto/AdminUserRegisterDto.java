package com.kepco.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUserRegisterDto {

    // =========================================================================
    // [1번 users 테이블 바인딩용 사원 기본 계정 데이터]
    // =========================================================================
    @NotBlank(message = "사원 아이디는 필수 입력 항목입니다.")
    @Size(min = 4, max = 50, message = "사원 아이디는 4자 이상 50자 이하로 입력해 주세요.")
    private String loginId;          // ⭕ DB: login_id

    @NotBlank(message = "초기 임시 비밀번호는 필수 입력 항목입니다.")
    @Size(min = 6, max = 100, message = "초기 비밀번호는 6자 이상 입력해 주세요.")
    private String password;         // ⭕ DB: password

    @NotBlank(message = "사원 실명은 필수 입력 항목입니다.")
    private String name;              // ⭕ DB: name

    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @NotBlank(message = "사원 이메일 주소는 필수 입력 항목입니다.")
    private String email;             // ⭕ DB: email (최종 스키마 반영 반영 완료)

    private String phone;             // ⭕ DB: phone (선택 항목)

    @NotBlank(message = "직무 권한 설정은 필수 항목입니다. (WORKER, DISPATCHER, HR, ADMIN)")
    private String role;              // ⭕ DB: role (CITIZEN을 제외한 사원 직무 부여)

    // =========================================================================
    // [2번 recovery_worker 테이블 바인딩용 데이터 - 💡 OpenAI 추천 핵심 소스]
    // =========================================================================
    @NotBlank(message = "사번은 필수 입력 항목입니다.")
    private String empNumber;         // ⭕ DB: emp_number

    private String department;        // ⭕ DB: department (소속 지사 및 부서)

    @NotBlank(message = "담당 구역 지정은 필수입니다.")
    private String assignedDistrict;  // ⭕ DB: assigned_district (OpenAI 지역 매칭용)

    private String certificate;       // ⭕ DB: certificate (OpenAI 자격증 분석용 - 예: 전기기사)
    
    private String grade;             // ⭕ DB: grade (OpenAI 숙련도 분석용 - 예: JUNIOR, MASTER)
}
