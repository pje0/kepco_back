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
            	    제공되는 [민원 정보]의 위험도와 난이도를 분석하고, 현재 출동 가능한 [작업자 명단]의 스펙(숙련도 직급, 자격증, 담당 구역)을 조합하여 가장 자원 효율적인 최적의 'matchScore'(0~100점)를 계산하세요.
            	    
            	    [엄격한 점수 산정 알고리즘 매칭 규칙 - 총점 100점 만점]
            	    
            	    1. 직급 및 기술 적합도 (최대 50점) - ★최우선 필수 기준★
            	       - 고난이도/위험 민원 (예: 변압기 스파크/폭발, 송전탑 지반 침하, 특고압 단선, 전압 강하 등):
            	         * MASTER / SENIOR 직급이면서 관련 핵심 자격증 소지자에게 45~50점 부여.
            	         * JUNIOR 직급은 위험 작업 수행이 불가하므로 10점 이하로 감점 처리.
            	       - 저난이도/단순 민원 (예: 스마트 계량기(AMI) 교체/오류/고장, 단순 통신 미수신, 가로등 점검, 까치집 제거 등):
            	         * JUNIOR 또는 SENIOR 직급에게 45~50점 최고점 부여.
            	         * MASTER 직급은 최고급 인력 낭비이자 심각한 오버스펙이므로, 예외 없이 무조건 '직급 및 기술 적합도' 점수를 0점 처리하세요. (아무리 지역이 일치해도 높은 점수를 받을 수 없도록 차단)
            	         
            	    2. 출동 지역 일치성 (최대 30점)
            	       - 민원 발생 지역(district)과 작업자의 담당 구역(assigned_district)이 완벽히 일치하면 30점 부여.
            	       - 인접 구역이거나 불일치 시 0점 부여.
            	       
            	    3. 자격증 및 추가 역량 (최대 20점)
            	       - 전기기사, 전기기능장, 소방설비기사 등 민원 내용과 직접 연관된 자격증 보유 시 가산점 부여.
            	    
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

