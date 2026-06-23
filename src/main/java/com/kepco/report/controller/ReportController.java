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
// ⚡ [근본 해결]: SecurityConfig 인가 규칙 및 테이블 스펙 물리 규격(/api/complaint)과 100% 일치하도록 매핑 주소 통합 수선
@RequestMapping({"/api/complaint", "/api/reports"})
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * 1. 시민 민원 접수 API
     */
    @PostMapping
    public ResponseEntity<Long> createReport(@RequestBody ReportCreateRequest request) {
        log.info("📢 [민원 접수 요청] 시민 ID: {}, 제목: {}", request.getCitizenId(), request.getTitle());
        Long reportId = reportService.createReport(request);
        log.info("✅ [민원 접수 완료] 생성된 민원 번호: {}", reportId);
        return ResponseEntity.ok(reportId);
    }

    /**
     * 2. ⚡ [연동 개혁 신설]: 파견 관제팀 전용 실시간 전체 민원 목록 조회 API
     * - 403 Forbidden을 원천 진압하고, 프론트엔드가 송출한 대문자 'status=PENDING' 규격을 완벽하게 가독하여
     *   JPA 비즈니스 레포지토리 쿼리에 톱니바퀴 싱크로 명중시킵니다.
     */
    @GetMapping
    public ResponseEntity<List<ReportResponse>> getAllReports(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "citizenId", required = false) Long citizenId) {
        
        log.info("📋 [관제팀 민원 목록 조회 요청] 상태필터: {}, 시민ID필터: {}", status, citizenId);
        
        // 🚀 서비스 레이어의 전체 조회 메서드 파이프라인 개통 (대문자 상태 규격 유기적 바인딩)
        List<ReportResponse> responses = reportService.getAllReports(status, citizenId);
        
        log.info("✅ [관제팀 민원 목록 조회 완료] 총 {}건의 마스터 데이터 반환", responses.size());
        return ResponseEntity.ok(responses);
    }

    /**
     * 3. 내 민원 조회 API (시민 전용)
     */
    @GetMapping("/my/{citizenId}")
    public ResponseEntity<List<ReportResponse>> getMyReports(@PathVariable("citizenId") Long citizenId) {
        log.info("🔍 [내 민원 조회 요청] 시민 ID: {}", citizenId);
        List<ReportResponse> responses = reportService.getMyReports(citizenId);
        log.info("✅ [내 민원 조회 완료] 총 {}건 반환", responses.size());
        return ResponseEntity.ok(responses);
    }
}
