package com.kepco.dashboard.service;

import com.kepco.dashboard.DTO.DashboardResponse;
//import com.kepco.employee.repository.RecoveryWorkerRepository; // v0.2 ERD 기준 복구팀 테이블
import com.kepco.report.repository.ReportRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
//@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    // 타 도메인의 Repository를 주입받아 통계를 산출합니다.
//    private final ReportRepository complaintRepository;
//    private final RecoveryWorkerRepository recoveryWorkerRepository;

    public DashboardResponse getDashboardStats() {
        // 1. 민원 통계 집계 (기본 제공 메서드 및 커스텀 메서드 활용)
//        long totalReports = complaintRepository.count();
//        long pending = complaintRepository.countByStatus("PENDING");
//        long inProgress = complaintRepository.countByStatus("IN_PROGRESS");
//        long completed = complaintRepository.countByStatus("RESOLVED");

        // 2. 직원 통계 집계
//        long available = recoveryWorkerRepository.countByWorkStatus("AVAILABLE");
//        long busy = recoveryWorkerRepository.countByWorkStatus("DISPATCHED");
//        long unavailable = recoveryWorkerRepository.countByWorkStatus("UNAVAILABLE");

        // 3. 차트용 데이터 집계 (Repository에 통계용 @Query 메서드를 만들어야 합니다)
        // TODO: QueryDSL이나 JPQL을 이용해 지역별/카테고리별 Count를 Map 형태로 묶어오는 로직 필요
//        Map<String, Long> reportsByDistrict = complaintRepository.countReportsByDistrict(); 
//        Map<String, Long> reportsByCategory = complaintRepository.countReportsByCategory();

        Map<String, Long> reportsByCategory = new HashMap<>();
        reportRepository.countReportsByCategory().forEach(stat -> 
            reportsByCategory.put(stat.getName(), stat.getValue())
        );

        // 4. 최종 DTO 조립 후 반환
        return DashboardResponse.builder()
//                .totalReports(totalReports)
//                .pendingReports(pending)
//                .inProgressReports(inProgress)
//                .completedReports(completed)
//                .availableEmployees(available)
//                .busyEmployees(busy)
//                .unavailableEmployees(unavailable)
//                .reportsByDistrict(reportsByDistrict)
//                .reportsByCategory(reportsByCategory)
                .build();
    }
}