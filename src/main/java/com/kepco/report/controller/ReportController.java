package com.kepco.report.controller;

import com.kepco.report.DTO.ReportCreateRequest;
import com.kepco.report.DTO.ReportResponse;
import com.kepco.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // 시민 민원 접수 API
    @PostMapping
    public ResponseEntity<Long> createReport(@RequestBody ReportCreateRequest request) {
        log.info("📢 [민원 접수 요청] 시민 ID: {}, 제목: {}", request.getCitizenId(), request.getTitle());
        Long reportId = reportService.createReport(request);
        log.info("✅ [민원 접수 완료] 생성된 민원 번호: {}", reportId);
        return ResponseEntity.ok(reportId);
    }

    // 내 민원 조회 API
    // 프론트엔드의 /api/reports/my/{id} 형식 대응
    @GetMapping("/my/{citizenId}")
    public ResponseEntity<List<ReportResponse>> getMyReports(@PathVariable("citizenId") Long citizenId) {
        log.info("🔍 [내 민원 조회 요청] 시민 ID: {}", citizenId);
        List<ReportResponse> responses = reportService.getMyReports(citizenId);
        log.info("✅ [내 민원 조회 완료] 총 {}건 반환", responses.size());
        return ResponseEntity.ok(responses);
    }
}