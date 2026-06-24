package com.kepco.work.controller;

import com.kepco.work.dto.WorkDispatchDto;
import com.kepco.work.service.WorkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/worker")
@RequiredArgsConstructor
public class WorkController {

    private final WorkService workService;

    // 내게 배정된 출동 목록 조회
    @GetMapping("/dispatches")
    public ResponseEntity<?> getMyDispatches(@AuthenticationPrincipal User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        List<WorkDispatchDto> list = workService.getMyDispatches(principal.getUsername());
        return ResponseEntity.ok(list);
    }
    
    @PatchMapping("/dispatches/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        try {
            workService.updateStatus(principal.getUsername(), id, body.get("status"), body.get("workNote"));
            return ResponseEntity.ok(Map.of("message", "상태가 변경되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}