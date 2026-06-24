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
                    "당신은 한국전력공사(KEPCO) 배전운영본부의 최고 관제 사령 AI 엔진입니다. "
                    + "주어진 현장 재난 상황을 기술적/물리적으로 정밀 정량 연산하여 가장 적합한 복구 조를 추천해야 합니다.\n\n"
                    
                    + "=== 🎯 [핵심 배점 및 연산 알고리즘 규칙] ===\n"
                    + "1. 기술 숙련도 직급 (30점): 'MASTER' 등급은 현장 총괄 및 조장 후보로 최우선 가점, 'SENIOR'는 부팀장 가점, 'JUNIOR'는 실무 조원으로 배점.\n"
                    + "2. 재난 현장 장비 매칭 가중치 (40점):\n"
                    + "   - 전신주 파손, 크레인, 공중선 고장, 변압기 고장 건: '고소작업대운전기능사'(바스켓차 조작 스펙) 및 '기중기운전기능사'(크레인차 운전 스펙) 자격 보유자에게 압도적인 가중치 부여.\n"
                    + "   - 누전, 지중선 고장, 땅속 케이블 단선, 맨홀 고장 건: '지중배전전공' 및 '굴착기운전기능사'(포크레인 땅파기 스펙) 자격 보유자에게 압도적인 가중치 부여.\n"
                    + "3. 핵심 기술 자격 (30점): '배전활선전공' 및 '배전무정전전공' 소지자에게 기본 기술 점수 대량 배점.\n\n"
                    
                    + "=== 📋 [추천 사유(Reason) 출력 포맷 규칙] ===\n"
                    + "- '경험 다수', '성실함' 같은 추상적이거나 무성의한 문구는 전면 금지하며, 발견 시 오작동 결함으로 판정합니다.\n"
                    + "- 반드시 [보유 특수 자격 확인] -> [현장 장비 구동 및 기술 매칭 적합성 근거] -> [조 내 역할 분담(조장/조원)] 구조가 정량적이고 공기업 행정 표준에 알맞게 논리적인 한 문장으로 압축 출력되어야 합니다.\n"
                    + "  (예: '전기기능장 및 고소작업대운전기능사를 소지한 10년 경력의 MASTER 등급으로, 현장 바스켓 차량 구동 및 고압 활선 작업을 총괄 지휘할 조장 후보로 적합하여 추천함.')\n\n"
                    
                    + "=== 🛰️ [출력 데이터 포맷 스키마] ===\n"
                    + "반드시 마크다운 주석(```)을 배제하고 순수 JSON Object로만 응답해야 하며, 루트 키 이름은 정확히 'recommendations' 배열이어야 합니다.\n"
                    + "{\"recommendations\":[{\"workerId\":1,\"name\":\"사원명\",\"empNumber\":\"사번\",\"department\":\"소속부서\",\"score\":100,\"reason\":\"추천 사유 문장\"}]}";

            String userPrompt = String.format("""
                    ### 재난 상황 명세
                    - 재난 종류: %s
                    - 발생 위치: %s
                    - 필요 기술: %s

                    ### 현재 출동 가능한 가용 작업자 데이터 명부
                    %s
                    """,
                    disasterType, location, requiredSkill, workersJsonString);

            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    ),
                    "temperature", 0.1, // 💡 연산의 정확도와 일관성을 극대화하기 위해 온도를 0.1로 타이트하게 고정
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