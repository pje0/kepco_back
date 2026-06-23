package com.kepco.dispatch.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kepco.auth.entity.User;
import com.kepco.auth.repository.UserRepository;
import com.kepco.dispatch.dto.AiWorkerRecommendResponseDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiMatchService {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Value("${openai.api.key:dummy-key-for-preventing-crash}")
    private String apiKey;

    @Value("${openai.model:gpt-4.1-mini}")
    private String model;

    private static final String OPENAI_API_URL =
            "https://api.openai.com/v1/chat/completions";

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    @Transactional(readOnly = true)
    public AiWorkerRecommendResponseDto recommendBestWorkers(
            String disasterType,
            String location,
            String requiredSkill) {

        log.info("@# AI 요원 추천 시작");
        log.info("@# 재난 종류: {}", disasterType);
        log.info("@# 발생 위치: {}", location);
        log.info("@# 필요 기술: {}", requiredSkill);

        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("dummy")) {
            log.error("@# OpenAI API KEY 미설정");
            throw new RuntimeException("OpenAI API KEY가 설정되지 않았습니다.");
        }

        List<User> allWorkers = userRepository.findAllEmployeesWithProfile()
                .stream()
                .filter(u -> "ROLE_WORKER".equalsIgnoreCase(u.getRole())
                        && u.getRecoveryWorker() != null)
                .collect(Collectors.toList());

        log.info("@# 조회된 작업자 수: {}", allWorkers.size());

        List<Map<String, Object>> workersData = allWorkers.stream().map(u -> {
            Map<String, Object> map = new HashMap<>();

            // ⚡ [근본 해결]: 상세 프로필 일련번호인 getRecoveryWorker().getId()를 과감히 폐기하고,
            // 실제 프론트엔드가 해독하여 바인딩할 사원 테이블(users)의 진짜 Primary Key인 u.getId() 축을 정밀 주입!
            map.put("workerId", u.getId()); 
            map.put("name", u.getName());
            map.put("empNumber", u.getEmpNumber() != null ? u.getEmpNumber() : "");
            map.put("department", u.getDepartment() != null ? u.getDepartment() : "");
            map.put("assignedDistrict", u.getRecoveryWorker().getAssignedDistrict());
            map.put("certificate", u.getRecoveryWorker().getCertificate());
            map.put("grade", u.getRecoveryWorker().getGrade());

            return map;
        }).collect(Collectors.toList());

        try {
            String workersJsonString =
                    objectMapper.writeValueAsString(workersData);

            String systemPrompt =
                    "당신은 한국전력공사(KEPCO)의 관제 AI입니다. "
                    + "재난 상황에 가장 적합한 복구 요원을 추천하세요. "
                    + "반드시 JSON만 응답해야 합니다. "
                    + "{\"recommendations\":[{\"workerId\":1,"
                    + "\"name\":\"홍길동\",\"empNumber\":\"EMP001\","
                    + "\"department\":\"송전부\",\"score\":95,"
                    + "\"reason\":\"배전 복구 경험 다수\"}]}";

            String userPrompt = String.format("""
                    ### 재난 상황
                    - 재난 종류: %s
                    - 발생 위치: %s
                    - 필요 기술: %s

                    ### 가용 작업자 명단
                    %s
                    """,
                    disasterType, location, requiredSkill, workersJsonString);

            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    ),
                    "temperature", 0.3,
                    "response_format", Map.of("type", "json_object")
            );

            String requestJson =
                    objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENAI_API_URL))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(
                            requestJson,
                            StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            log.info("@# OpenAI 응답 상태 코드: {}", response.statusCode());

            if (response.statusCode() != 200) {
                log.error("@# OpenAI API 호출 실패");
                log.error("@# 상태 코드: {}", response.statusCode());
                log.error("@# 응답 BODY: {}", response.body());

                throw new RuntimeException(
                        "OpenAI API 호출 실패 - status: "
                                + response.statusCode());
            }

            JsonNode rootNode = objectMapper.readTree(response.body());
            JsonNode choicesNode = rootNode.get("choices");

            if (choicesNode == null || choicesNode.isEmpty()) {
                throw new RuntimeException(
                        "OpenAI 응답에 choices 데이터가 없습니다.");
            }

            String messageContent = choicesNode.get(0)
                    .get("message")
                    .get("content")
                    .asText();

            log.info("@# AI 추천 결과 원문: {}", messageContent);

            return objectMapper.readValue(
                    messageContent,
                    AiWorkerRecommendResponseDto.class);

        } catch (Exception e) {
            log.error("@# OpenAI 기반 요원 추천 중 예외 발생", e);
            throw new RuntimeException(
                    "AI 요원 추천 연산 실패: " + e.getMessage(), e);
        }
    }
}
