package com.kepco.dispatch.controller;

import com.kepco.dispatch.dto.DispatchDashboardDto;
import com.kepco.dispatch.dto.DispatchDashboardDto.DispatchItem; 
import com.kepco.dispatch.dto.DispatchCreateRequestDto;
import com.kepco.dispatch.dto.PendingComplaintDto;
import com.kepco.dispatch.dto.AvailableWorkerDto;
import com.kepco.dispatch.dto.HistorySearchRequestDto;
import com.kepco.dispatch.service.DispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
     * 1. 관제 시스템 과거 완료 이력 고속 복합 페이징 조회 API
     * - ⚡ [아키텍처 근본 개혁 - 최상단 배치]: 스프링 부트의 경로 가로채기 간섭 결함({id} 우회 버그)을 
     *   원천적으로 차단하기 위해, 고정 주소 명세인 /history 매핑을 컨트롤러 맨 위 구역으로 전격 격상 이동!
     * - 💡 [하이브리드 주소 매핑]: 팀 브랜치 간 슬래시 꼬임 방지를 위해 공백 루트와 매핑 병렬 지원
     */
    @GetMapping({"/history", "history"})
    public ResponseEntity<Page<DispatchItem>> getHistory(
            HistorySearchRequestDto requestDto,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        
        log.info("@# DispatchController - 과거 완료 이력 복합 페이징 조회 요청 수신");
        Page<DispatchItem> historyPage = dispatchService.getDispatchHistory(requestDto, pageable);
        return ResponseEntity.ok(historyPage);
    }

    /**
     * 2. [수선 완결] 파견 관리 대시보드 데이터 및 카운트 전체 조회
     * - URL: GET /api/dispatch 또는 GET /api/dispatch/dashboard
     */
    @GetMapping({"", "/dashboard"})
    public ResponseEntity<DispatchDashboardDto> getDashboardData() {
        log.info("@# DispatchController - 실시간 파견 대시보드 통계 및 목록 조회 요청 수신");
        DispatchDashboardDto dashboardData = dispatchService.getDashboardData();
        return ResponseEntity.ok(dashboardData);
    }

    /**
     * 3. 미배정 대기 신고 건 목록 조회 (드롭다운 연동)
     */
    @GetMapping("/pending-complaints")
    public ResponseEntity<List<PendingComplaintDto>> getPendingComplaints() {
        log.info("@# DispatchController - 미배정 대기 신고 건 목록 조회 요청 수신");
        List<PendingComplaintDto> list = dispatchService.getPendingComplaints();
        return ResponseEntity.ok(list);
    }

    /**
     * 4. 가용 출동 요원 목록 조회 (드롭다운 연동)
     */
    @GetMapping("/available-workers")
    public ResponseEntity<List<AvailableWorkerDto>> getAvailableWorkers() {
        log.info("@# DispatchController - 가용 출동 요원 목록 조회 요청 수신");
        List<AvailableWorkerDto> list = dispatchService.getAvailableWorkers();
        return ResponseEntity.ok(list);
    }

    /**
     * 5. 신규 현장 파견 지시 하달 (최종 '파견 지시' 버튼 연동)
     */
    @PostMapping
    public ResponseEntity<?> createDispatch(@RequestBody DispatchCreateRequestDto requestDto,
                                            @AuthenticationPrincipal User principal) {
        log.info("@# DispatchController - 신규 파견 지시 요청 수신 (민원ID: {}, 요원ID: {})", requestDto.getComplaintId(), requestDto.getWorkerId());
        try {
            String dispatcherUsername = (principal != null) ? principal.getUsername() : "admin";
            dispatchService.createDispatch(requestDto, dispatcherUsername);
            return ResponseEntity.ok(Map.of("message", "현장 복구팀에 파견 지시가 성공적으로 하달되었습니다."));
        } catch (Exception e) {
            log.error("@# DispatchController - 파견 지시 저장 실패", e);
            return ResponseEntity.badRequest().body(Map.of("message", "파견 지시 실패: " + e.getMessage()));
        }
    }

    /**
     * 6. [수선 완결] 현장 복구 완료 및 파견 종료 처리 ('완료 처리' 버튼 연동)
     * - ⚡ 정적 경로인 /history 보다 하단에 배치하여 변수 {id} 흡수 충돌 결함을 원천 방어 완료!
     * - URL: PATCH /api/dispatch/{id}/status
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> completeDispatch(@PathVariable("id") Long dispatchId,
                                              @RequestBody Map<String, String> requestBody) {
        try {
            String workNote = requestBody.get("workNote");
            log.info("@# DispatchController - 파견 완료 처리 요청 수신 (파견ID: {})", dispatchId);
            dispatchService.completeDispatch(dispatchId, workNote);
            return ResponseEntity.ok(Map.of("message", "현장 복구 및 파견 종료 처리가 완료되었습니다."));
        } catch (Exception e) {
            log.error("@# DispatchController - 파견 상태 변경 실패", e);
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
