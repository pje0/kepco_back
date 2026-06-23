package com.kepco.report.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kepco.report.DTO.ReportAiClassificationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportAiService {

    private final ObjectMapper objectMapper;

    @Value("${openai.api.key:dummy-key-for-preventing-crash}")
    private String apiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    private static final String OPENAI_API_URL =
            "https://api.openai.com/v1/chat/completions";

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * 🧠 [민원 패키지 전용 AI 엔진]
     * - 민원 접수 시 제목과 내용을 판독하여 KEPCO 표준 카테고리와 재난 심각도를 정형화 판독합니다.
     */
    public ReportAiClassificationDto classifyReport(String title, String content) {
        log.info("@# ReportAiService - 민원 도메인 내부 OpenAI 실시간 심각도/카테고리 판독 가동");

        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("dummy")) {
            log.error("@# OpenAI API KEY 미설정");
            throw new RuntimeException("OpenAI API KEY가 설정되지 않았습니다.");
        }

        try {
            String systemPrompt = "당신은 한국전력공사(KEPCO)의 민원 자동 분류 관제 AI입니다. "
                    + "시민이 입력한 민원의 제목과 내용을 정밀 분석하여 가장 적절한 aiCategory와 aiPriority(심각도)를 판별하세요. "
                    + "반드시 다음 7대 카테고리와 3대 심각도 코드값 규칙으로만 응답해야 합니다. "
                    + "[aiCategory 분류 철칙]: '정전', '계량기 고장', '변압기 이상', '전선 단선', '지중 설비 이상', '누전 및 감전 위험', '선로 이물질 및 수목 접촉', '기타' 중 하나로만 매핑하세요. "
                    + "🚨 특히 까치집, 새둥지, 풍선, 비닐, 혹은 가로수 나뭇가지가 전선이나 전주에 걸렸다는 신고는 무조건 '선로 이물질 및 수목 접촉'으로 분류해야 합니다. "
                    + "[aiPriority 심각도 철칙]: "
                    + "- 'CRITICAL': 아파트 단지 전체 정전, 변전소 화재, 고압선 단선, 누전/감전 위험 등 광범위한 재난 또는 인명 안전 우려 건 "
                    + "- 'MAJOR': 일부 상가 정전, 변압기 과부하 징후, 까치집 내 철사 줄 노출이나 선로 불꽃 감지 등 즉각 출동이 필요한 예방 건 "
                    + "- 'MINOR': 가정집 단독 계량기 고장, 단순 가로등 깜빡임, 단순 까치집 축조 시작 등 경미한 건 "
                    + "반드시 다음 JSON 포맷 규격으로만 응답하며 다른 텍스트나 설명, 마크다운은 절대 금지합니다: "
                    + "{\"aiCategory\": \"정전\", \"aiPriority\": \"CRITICAL\"}";

            String userPrompt = String.format("### 시민 접수 민원 원문\n- 제목: %s\n- 내용: %s", title, content);

            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    ),
                    "temperature", 0.1,
                    "response_format", Map.of("type", "json_object")
            );

            String requestJson = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENAI_API_URL))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                throw new RuntimeException("OpenAI API 호출 실패 - status: " + response.statusCode());
            }

            JsonNode rootNode = objectMapper.readTree(response.body());
            String messageContent = rootNode.get("choices").get(0).get("message").get("content").asText();

            log.info("@# OpenAI 민원 도메인 자체 판독 결과 원문: {}", messageContent);

            return objectMapper.readValue(messageContent, ReportAiClassificationDto.class);

        } catch (Exception e) {
            log.error("@# OpenAI 민원 실시간 분류 연산 중 예외 발생", e);
            ReportAiClassificationDto fallback = new ReportAiClassificationDto();
            fallback.setAiCategory("기타");
            fallback.setAiPriority("MINOR");
            return fallback;
        }
    }
}
