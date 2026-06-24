// src/main/java/com/kepco/dashboard/service/DashboardService.java
package com.kepco.dashboard.service;

import com.kepco.dashboard.DTO.DashboardResponse;
import com.kepco.dispatch.repository.RecoveryWorkerRepository;
import com.kepco.report.repository.ReportRepository;
import com.kepco.report.repository.ReportRepository.StatCount;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final ReportRepository reportRepository;
    private final RecoveryWorkerRepository recoveryWorkerRepository;

    public DashboardResponse getDashboardStats() {
        // 1. 민원 통계 집계 (전체 건수는 0 처리, 완료 건수는 최근 7일 기준으로 변경)
        long totalReports = 0; 
        long pending = reportRepository.countByStatus("PENDING");
        long inProgress = reportRepository.countByStatus("IN_PROGRESS");
        long completed = reportRepository.countResolvedLast7Days();

        // 2. 직원 통계 집계
        long available = recoveryWorkerRepository.countByWorkStatus("AVAILABLE");
        long busy = recoveryWorkerRepository.countByWorkStatus("DISPATCHED");
        long unavailable = recoveryWorkerRepository.countByWorkStatus("UNAVAILABLE");

        // 3. 차트용 데이터 집계 (ReportRepository의 StatCount 프로젝션 배열을 Map 객체로 변환)
        Map<String, Long> reportsByDistrict = new LinkedHashMap<>();
        for (StatCount stat : reportRepository.countReportsByDistrict()) {
            if (stat.getName() != null) reportsByDistrict.put(stat.getName(), stat.getValue());
        }

        Map<String, Long> reportsByCategory = new LinkedHashMap<>();
        for (StatCount stat : reportRepository.countReportsByCategory()) {
            if (stat.getName() != null) reportsByCategory.put(stat.getName(), stat.getValue());
        }

        Map<String, Long> monthlyReports = new LinkedHashMap<>();
        for (StatCount stat : reportRepository.countReportsByMonth()) {
            if (stat.getName() != null) monthlyReports.put(stat.getName(), stat.getValue());
        }

        // 4. 최종 DTO 조립 후 반환
        return DashboardResponse.builder()
                .totalReports(totalReports)
                .pendingReports(pending)
                .inProgressReports(inProgress)
                .completedReports(completed)
                .availableEmployees(available)
                .busyEmployees(busy)
                .unavailableEmployees(unavailable)
                .reportsByDistrict(reportsByDistrict)
                .reportsByCategory(reportsByCategory)
                .monthlyReports(monthlyReports)
                .build();
    }
}