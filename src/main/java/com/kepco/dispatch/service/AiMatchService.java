package com.kepco.dispatch.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kepco.dispatch.dto.AiWorkerRecommendResponseDto;
import com.kepco.auth.entity.User;
import com.kepco.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiMatchService {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    // 💡 [교정]: application.yml에 키가 선언되어 있지 않더라도 서버가 터지지 않도록 디폴트 방어 속성(:) 주입
    @Value("${openai.api-key:dummy-key-for-preventing-crash}")
    private String apiKey;

    @Value("${openai.model:gpt-4o}")
    private String model;

    /**
     * 🧠 [OpenAI 코어 엔진] 재난 상황 발생 시 정형화 명부 기반 최적 요원 스코어링 추천
     */
    @Transactional(readOnly = true)
    public AiWorkerRecommendResponseDto recommendBestWorkers(String disasterType, String location, String requiredSkill) {
        log.info("@# AiMatchService - OpenAI 기반 현장 복구팀 실시간 스코어링 매칭 연산 가동");

        List<User> allWorkers = userRepository.findAllEmployeesWithProfile().stream()
                .filter(u -> "ROLE_WORKER".equalsIgnoreCase(u.getRole()) && u.getRecoveryWorker() != null)
                .collect(Collectors.toList());

        List<Map<String, Object>> workersData = allWorkers.stream().map(u -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("workerId", u.getRecoveryWorker().getId()); 
            map.put("name", u.getName());
            map.put("empNumber", u.getEmpNumber() != null ? u.getEmpNumber() : "");
            map.put("department", u.getDepartment() != null ? u.getDepartment() : "");
            map.put("assignedDistrict", u.getRecoveryWorker().getAssignedDistrict());
            map.put("certificate", u.getRecoveryWorker().getCertificate());
            map.put("grade", u.getRecoveryWorker().getGrade());
            return map;
        }).collect(Collectors.toList());

        try {
            String workersJsonString = objectMapper.writeValueAsString(workersData);

            String systemPrompt = "당신은 한국전력공사(KEPCO)의 관제 AI입니다. "
                    + "주어진 사원 명단에서 재난 상황(위치, 필요기술)에 가장 적합한 요원을 선별해 스코어링 하세요. "
                    + "반드시 다음 JSON 포맷 규격으로만 응답해야 하며, 다른 텍스트는 절대 금지합니다: "
                    + "{\"recommendations\": [{\"workerId\": 1, \"name\": \"홍길동\", \"empNumber\": \"EMP-01\", \"department\": \"부서\", \"score\": 95, \"reason\": \"이유\"}]}";

            String userPrompt = String.format("### 재난 상황\n- 종류: %s\n- 발생 위치: %s\n- 필요 기술: %s\n\n### 가용 사원 명단\n%s", 
                    disasterType, location, requiredSkill, workersJsonString);

            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    ),
                    "response_format", Map.of("type", "json_object") 
            );

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://openai.com"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            Map<String, Object> jsonResponse = objectMapper.readValue(response.body(), Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) jsonResponse.get("choices");
            String messageContent = (String) ((Map<String, Object>) choices.get(0).get("message")).get("content");

            return objectMapper.readValue(messageContent, AiWorkerRecommendResponseDto.class);

        } catch (Exception e) {
            log.error("@# OpenAI 매칭 연산 중 치명적 예외 발생", e);
            throw new RuntimeException("AI 요원 추천 연산에 실패했습니다. " + e.getMessage());
        }
    }
}
