package com.kepco.dashboard.service;

import com.kepco.dashboard.DTO.DashboardResponse;
import com.kepco.report.repository.ReportRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    // 🚨 RecoveryWorker 관련 레포지토리는 아직 없으므로 제외하고 Report만 주입받습니다.
    private final ReportRepository reportRepository;

    public DashboardResponse getDashboardStats() {
        // 1. 민원 통계 집계 (진짜 DB에서 Count)
        long totalReports = reportRepository.count();
        long pending = reportRepository.countByStatus("pending");
        long inProgress = reportRepository.countByStatus("in_progress");
        long completed = reportRepository.countByStatus("resolved");

        // 2. 직원 통계 집계 (🚨 RecoveryWorker 엔티티가 없으므로 임시로 0 고정하여 에러 방지)
        long available = 0L;
        long busy = 0L;
        long unavailable = 0L;

        // 3. 차트용 데이터 집계 (DB Group By 결과를 Map으로 예쁘게 변환)
        Map<String, Long> reportsByDistrict = new HashMap<>();
        reportRepository.countReportsByDistrict().forEach(stat -> 
            reportsByDistrict.put(stat.getName(), stat.getValue())
        );

        Map<String, Long> reportsByCategory = new HashMap<>();
        reportRepository.countReportsByCategory().forEach(stat -> 
            reportsByCategory.put(stat.getName(), stat.getValue())
        );

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
                .build();
    }
}