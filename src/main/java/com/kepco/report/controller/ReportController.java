package com.kepco.report.controller;

import com.kepco.report.DTO.ReportCreateRequest;
import com.kepco.report.DTO.ReportResponse;
import com.kepco.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // 시민 민원 접수 API
    @PostMapping
    public ResponseEntity<Long> createReport(@RequestBody ReportCreateRequest request) {
        Long reportId = reportService.createReport(request);
        return ResponseEntity.ok(reportId);
    }

    // 내 민원 조회 API
    // 프론트엔드의 /api/reports/citizen/{id} 형식 대응
    @GetMapping("/citizen/{citizenId}")
    public ResponseEntity<List<ReportResponse>> getMyReports(@PathVariable Long citizenId) {
        List<ReportResponse> responses = reportService.getMyReports(citizenId);
        return ResponseEntity.ok(responses);
    }
}