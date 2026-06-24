package com.kepco.dispatch.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DispatchDashboardDto {
    // 1. 상단 통계 카드용 카운트 정보
    private long pendingCount;       // UI: '대기신고' 숫자 (status = 'pending')
    private long activeCount;        // UI: '출동중' 숫자 (status = 'assigned' 또는 'dispatched')
    private long completedCount;     // UI: '완료' 숫자 (status = 'completed')
    private long availableWorkers;   // UI: '가용 요원' 숫자 (work_status = 'AVAILABLE')
    
    // 2. 하단 메인 테이블 목록
    private List<DispatchItem> dispatchList;

    @Data
    public static class DispatchItem {
        private Long dispatchId;            // DB: dispatch.id -> 완료 처리 버튼 누를 때 필요
        private String complaintTitle;      // UI: '신고 건'
        private String workerName;          // UI: '출동요원'
        private LocalDateTime assignedAt;    // UI: '파견 시각'
        private String workNote;            // UI: '지시 사항'
        private String status;              // UI: '상태' (assigned, completed 등)
        private LocalDateTime completedAt; 
    }
}
