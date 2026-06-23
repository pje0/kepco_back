package com.kepco.dispatch.dto;

import lombok.Data;
import java.util.List;

@Data
public class AiWorkerRecommendResponseDto {

    private List<RecommendedWorker> recommendations;

    @Data
    public static class RecommendedWorker {
        private Long workerId;          // ⭕ [교정 완료]: 실제 DB 키 스펙인 recovery_worker.id 매핑용 변수명 싱크 조율
        private String name;            // 사원 실명
        private String empNumber;       // 사번
        private String department;      // 소속 부서
        private int score;              // AI가 연산한 추천 점수 (0~100)
        private String reason;          // AI의 정형화된 추천 사유
    }
}
