package com.kepco.complaint.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kepco.complaint.dto.AiAnalysisResult;
import com.kepco.complaint.dto.ComplaintRequestDto;
import com.kepco.complaint.dto.RecommendedWorkerResponseDto;
import com.kepco.complaint.entity.Complaint;
import com.kepco.complaint.repository.ComplaintRepository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final ComplaintAiService complaintAiService;
    private final EntityManager entityManager;

    @Transactional
    public void registerComplaint(ComplaintRequestDto dto) {
        complaintRepository.save(dto.toEntity());
    }

    public List<RecommendedWorkerResponseDto> getRecommendedWorkers(Long complaintId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("해당 민원이 존재하지 않습니다."));

        // 💡 [최종 교정]: 배열 인덱스(row[0]) 캐스팅 에러를 완벽하게 방어하기 위해 ALIAS 별칭 매핑 구조로 변경합니다.
        String sql = """
            SELECT 
                w.id as worker_id, 
                u.name as name, 
                w.emp_number as emp_number, 
                w.department as department, 
                w.assigned_district as assigned_district, 
                w.grade as grade, 
                w.certificate as certificate 
            FROM recovery_worker w
            JOIN users u ON w.user_id = u.id
            WHERE w.work_status = 'AVAILABLE'
        """;
        
        // 하이버네이트 고유의 결과 변환기를 결합하여 Map 리스트 구조로 안전하게 추출
        org.hibernate.query.NativeQuery<?> nativeQuery = (org.hibernate.query.NativeQuery<?>) entityManager.createNativeQuery(sql);
        nativeQuery.setResultTransformer(org.hibernate.transform.Transformers.ALIAS_TO_ENTITY_MAP);
        
        List<?> rows = nativeQuery.getResultList();

        List<Map<String, Object>> availableWorkers = new ArrayList<>();
        Map<Long, RecommendedWorkerResponseDto> workerBaseMap = new HashMap<>();

        for (Object obj : rows) {
            @SuppressWarnings("unchecked")
            Map<String, Object> row = (Map<String, Object>) obj;
            
            // 데이터베이스 드라이버별 숫자 타입(BigInt -> Long/Integer) 크래시 완벽 방어
            Long id = ((Number) row.get("worker_id")).longValue();
            String name = (String) row.get("name");
            String empNum = (String) row.get("emp_number");
            String dept = (String) row.get("department");
            String district = (String) row.get("assigned_district");
            String grade = (String) row.get("grade");
            String cert = (String) row.get("certificate");

            availableWorkers.add(Map.of(
                "workerId", id, "name", name, "assigned_district", district, "grade", grade, "certificate", cert != null ? cert : ""
            ));

            workerBaseMap.put(id, new RecommendedWorkerResponseDto(
                id, name, empNum, dept, district, grade, cert, 0, ""
            ));
        }

        // 1차 추출된 가용 기사가 진짜 몇 명인지 눈으로 확인하는 핵심 디버깅 로그
        log.info("🚨 [가용 자원 진단 콘솔] 현재 출동 대기 중인 1차 기사 수: {}명", availableWorkers.size());

        if (availableWorkers.isEmpty()) return new ArrayList<>();

        // 2. OpenAI 연동 매칭 엔진 가동
        List<AiAnalysisResult> aiResults = complaintAiService.getAiRecommendations(complaint, availableWorkers);

        // 3. AI 연산 결과와 원본 기사 정보 매핑 조인
        List<RecommendedWorkerResponseDto> finalRecommendations = new ArrayList<>();
        for (AiAnalysisResult ai : aiResults) {
            RecommendedWorkerResponseDto base = workerBaseMap.get(ai.workerId());
            if (base != null) {
                finalRecommendations.add(new RecommendedWorkerResponseDto(
                    base.workerId(), base.name(), base.empNumber(), base.department(),
                    base.assignedDistrict(), base.grade(), base.certificate(),
                    ai.matchScore(), ai.recommendationReason()
                ));
            }
        }

        // 매칭 스코어 높은 순 정렬
        finalRecommendations.sort((a, b) -> Integer.compare(b.matchScore(), a.matchScore()));
        return finalRecommendations;
    }
}