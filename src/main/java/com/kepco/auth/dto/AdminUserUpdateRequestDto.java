package com.kepco.auth.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data // Getter, Setter, ToString을 한 번에 퉁치기 완료
public class AdminUserUpdateRequestDto {

    // [1번 users 테이블 기본 신상 수정용 파라미터]
    private String name;              // 사원 실명 변경
    private String role;
    
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;             // 사원 이메일 변경
    
    private String phone;             // 사원 연락처 변경

    // [2번 recovery_worker 테이블 수정용 파라미터 - OpenAI 추천 핵심 스펙]
    private String department;        // 소속 지사 및 부서 변경
    private String assignedDistrict;  // 담당 구역 변경 (OpenAI 지역 매칭용)
    private String certificate;       // 보유 자격증 목록 갱신 (OpenAI 자격 검증용)
    private String grade;             // 직급 진급 반영 (JUNIOR -> SENIOR 등)
}
