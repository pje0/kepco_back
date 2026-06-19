package com.kepco.dispatch.controller;

import com.kepco.dispatch.dto.AiWorkerRecommendResponseDto;
import com.kepco.dispatch.service.AiMatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AiMatchController {

    private final AiMatchService aiMatchService;

    /**
     * 🧠 [신규 추가] 관제센터용 OpenAI 실시간 복구 요원 추천 매칭 API
     * - URL 규격: GET /api/dispatch/ai-recommend
     */
    @GetMapping("/api/dispatch/ai-recommend")
    public ResponseEntity<?> getAiRecommendedWorkers(
            @RequestParam("disasterType") String disasterType,
            @RequestParam("location") String location,
            @RequestParam("requiredSkill") String requiredSkill) {
        
        log.info("@# AiMatchController - 관제센터 AI 요원 추천 요청 수신 (위치: {}, 필요기술: {})", location, requiredSkill);
        
        try {
            AiWorkerRecommendResponseDto recommendations = aiMatchService.recommendBestWorkers(disasterType, location, requiredSkill);
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            log.error("@# AiMatchController - OpenAI 추천 연산 중 예외 터짐", e);
            return ResponseEntity.internalServerError().body(Map.of("message", "AI 매칭 연산 실패: " + e.getMessage()));
        }
    }
}
