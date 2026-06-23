package com.kepco.complaint.dto;

public record RecommendedWorkerResponseDto(
    Long workerId,              // recovery_worker 테이블의 PK
    String name,                // users 테이블의 기사 실명
    String empNumber,           // 사번
    String department,          // 소속 지사/부서
    String assignedDistrict,    // 담당 구역 (전담 구 이름)
    String grade,               // 숙련도 직급 (JUNIOR, SENIOR, MASTER)
    String certificate,         // 보유 자격증 목록
    int matchScore,             // OpenAI가 연산한 적합도 점수 (0~100)
    String recommendationReason // OpenAI가 작성한 팩트 기반 추천 사유
) {}
