package com.kepco.dashboard.DTO;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class DashboardResponse {
    // 1. 민원 상태 통계
    private long totalReports;
    private long pendingReports;
    private long inProgressReports;
    private long completedReports;

    // 2. 직원(복구팀) 상태 통계
    private long availableEmployees;
    private long busyEmployees;
    private long unavailableEmployees;

    // 3. 차트용 데이터 (프론트엔드의 Object.entries() 처리를 위해 Map 형태 반환)
    private Map<String, Long> reportsByDistrict;
    private Map<String, Long> reportsByCategory;
    private Map<String, Long> monthlyReports;
}