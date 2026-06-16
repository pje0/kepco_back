package com.kepco.complaint.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kepco.complaint.dto.AiAnalysisResult;
import com.kepco.complaint.entity.Complaint;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ComplaintAiService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${openai.api.key}")
    private String apiKey;

    public ComplaintAiService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build(); // baseUrl() 제거
        this.objectMapper = objectMapper;
    }

    public List<AiAnalysisResult> getAiRecommendations(Complaint complaint, List<Map<String, Object>> availableWorkers) {
        try {
            String workersJson = objectMapper.writeValueAsString(availableWorkers);

            String systemPrompt = """
                당신은 한국전력공사의 베테랑 파견 관제 AI 시스템입니다. 
                제공되는 [민원 정보]의 위험도와 난이도를 분석하고, 현재 출동 가능한 [작업자 명단]의 스펙(숙련도 직급, 자격증, 담당 구역)을 매칭하세요.
                
                [비즈니스 매칭 규칙]
                1. 난이도가 낮은 민원(예: 까치집 제거, 가로등 점검)은 JUNIOR 직급 기사를 우선 매칭하여 고급 인력 낭비를 방지하세요.
                2. 난이도가 높거나 위험한 민원(예: 변압기 폭발, 고압선 단선)은 반드시 MASTER 또는 SENIOR 직급의 숙련자와 핵심 자격증 소지자를 강력히 추천하세요.
                3. 민원 발생 지역(district)과 작업자의 담당 구역(assigned_district)이 일치하면 높은 가산점을 부여하세요.
                
                [출력 형식 가이드]
                반드시 다른 설명 없이 순수한 JSON Array 형식으로만 응답해야 합니다. 마크다운(```json)도 절대 포함하지 마세요.
                응답 스키마: [{"workerId": 1, "matchScore": 95, "recommendationReason": "추천 사유 명세"}]
                """;

            String userPrompt = """
                [민원 정보]
                - 제목: %s
                - 내용: %s
                - 지역: %s
                
                [출동 가능 작업자 명단]
                %s
                """.formatted(complaint.getTitle(), complaint.getContent(), complaint.getDistrict(), workersJson);

            Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.2
            );

            String responseString = webClient.post()
            	.uri("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            Map<String, Object> responseMap = objectMapper.readValue(responseString, new TypeReference<>() {});
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            String aiJsonResult = (String) ((Map<String, Object>) choices.get(0).get("message")).get("content");

            return objectMapper.readValue(aiJsonResult.trim(), new TypeReference<List<AiAnalysisResult>>() {});

         // ComplaintAiService.java 맨 하단 catch 블록 수정
        } catch (Exception e) {
            // 🎯 [여기 교체!]: 조용히 묻히던 에러 원인을 콘솔에 강제로 출력시킵니다.
            log.error("🚨 [OpenAI 연동 폭발 콘솔] 에러 진짜 원인: ", e);
            return new ArrayList<>();
        }
    }
}

