package com.kepco.dispatch.controller;

import com.kepco.dispatch.dto.DispatchDashboardDto;
import com.kepco.dispatch.dto.DispatchCreateRequestDto;
import com.kepco.dispatch.dto.PendingComplaintDto;
import com.kepco.dispatch.dto.AvailableWorkerDto;
import com.kepco.dispatch.service.DispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dispatch")
@RequiredArgsConstructor
@Slf4j
public class DispatchController {

    private final DispatchService dispatchService;

    /**
     * 1. 파견 관리 대시보드 데이터 및 카운트 전체 조회
     * - URL: GET /api/dispatch/dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DispatchDashboardDto> getDashboardData() {
        log.info("@# DispatchController - 대시보드 통계 및 목록 조회 요청 수신");
        DispatchDashboardDto dashboardData = dispatchService.getDashboardData();
        return ResponseEntity.ok(dashboardData);
    }

    /**
     * 2. 미배정 대기 신고 건 목록 조회 (드롭다운 연동)
     * - URL: GET /api/dispatch/pending-complaints
     */
    @GetMapping("/pending-complaints")
    public ResponseEntity<List<PendingComplaintDto>> getPendingComplaints() {
        List<PendingComplaintDto> list = dispatchService.getPendingComplaints();
        return ResponseEntity.ok(list);
    }

    /**
     * 3. 가용 출동 요원 목록 조회 (드롭다운 연동)
     * - URL: GET /api/dispatch/available-workers
     */
    @GetMapping("/available-workers")
    public ResponseEntity<List<AvailableWorkerDto>> getAvailableWorkers() {
        List<AvailableWorkerDto> list = dispatchService.getAvailableWorkers();
        return ResponseEntity.ok(list);
    }

    /**
     * 4. 신규 현장 파견 지시 하달 (최종 '파견 지시' 버튼 연동)
     * - URL: POST /api/dispatch
     */
    @PostMapping
    public ResponseEntity<?> createDispatch(@RequestBody DispatchCreateRequestDto requestDto,
                                            @AuthenticationPrincipal User principal) {
        log.info("@# DispatchController - 신규 파견 지시 요청 수신 (민원ID: {}, 요원ID: {})", requestDto.getComplaintId(), requestDto.getWorkerId());
        try {
            // 현재 로그인한 관제사의 계정명을 넘겨 관제사 ID를 매핑합니다.
            String dispatcherUsername = (principal != null) ? principal.getUsername() : "admin";
            dispatchService.createDispatch(requestDto, dispatcherUsername);
            return ResponseEntity.ok(Map.of("message", "현장 복구팀에 파견 지시가 성공적으로 하달되었습니다."));
        } catch (Exception e) {
            log.error("@# DispatchController - 파견 지시 저장 실패", e);
            return ResponseEntity.badRequest().body(Map.of("message", "파견 지시 실패: " + e.getMessage()));
        }
    }

    /**
     * 5. 현장 복구 완료 및 파견 종료 처리 ('완료 처리' 버튼 연동)
     * - URL: PUT /api/dispatch/{id}/complete
     */
    @PutMapping("/{id}/complete")
    public ResponseEntity<?> completeDispatch(@PathVariable("id") Long dispatchId,
                                              @RequestBody Map<String, String> requestBody) {
        try {
            String workNote = requestBody.get("workNote");
            dispatchService.completeDispatch(dispatchId, workNote);
            return ResponseEntity.ok(Map.of("message", "현장 복구 및 파견 종료 처리가 완료되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
