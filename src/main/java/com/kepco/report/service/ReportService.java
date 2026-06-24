package com.kepco.report.service;

import com.kepco.report.DTO.ReportCreateRequest;
import com.kepco.report.DTO.ReportResponse;
import com.kepco.report.entity.Report;
import com.kepco.report.repository.ReportRepository;
import com.kepco.report.DTO.ReportAiClassificationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    
    // 🌟 [독립 모듈 주입]: 순수 민원 패키지 내부 전용 AI 서비스를 연결합니다.
    private final ReportAiService reportAiService;

    // 민원 접수 (Create)
    @Transactional
    public Long createReport(ReportCreateRequest request) {
        log.info("⚙️ 민원 데이터 엔티티 변환 및 DB 저장 시작...");
        Report report = Report.builder()
                .citizenId(request.getCitizenId())
                .title(request.getTitle())
                .content(request.getContent())
                .region("부산광역시") // 기본값 세팅
                .district(request.getDistrict())
                .address(request.getAddress())
                .build();

        Long savedId = reportRepository.save(report).getId();
        log.info("⚙️ 민원 데이터 DB 저장 완료 (ID: {})", savedId);
        
        // 🌟 [AI 연쇄 적재 로직 추가]: 엔티티 원본 수정을 방어하며 비어있던 DB 컬럼을 즉시 갱신합니다.
        try {
            ReportAiClassificationDto aiResult = reportAiService.classifyReport(request.getTitle(), request.getContent());
            reportRepository.updateAiClassification(savedId, aiResult.getAiCategory(), aiResult.getAiPriority());
            log.info("⚙️ AI 분석 데이터 원본 테이블 적재 완료 -> 카테고리: [{}], 심각도: [{}]", aiResult.getAiCategory(), aiResult.getAiPriority());
        } catch (Exception e) {
            log.error("🚨 AI 분석 실패 -> 시스템 안정성을 위해 기본값으로 방어 적재합니다.", e);
            reportRepository.updateAiClassification(savedId, "기타", "MINOR");
        }
        
        return savedId;
    }

    /**
     * ⚡ [연동 개혁 신설]: 파견 관제팀 전용 전체 민원 목록 실시간 동적 쿼리 조회
     * - 프론트엔드가 대문자로 포맷팅한 status 파라미터가 비즈니스 쿼리축에 완벽히 명중되도록 가동
     */
    public List<ReportResponse> getAllReports(String status, Long citizenId) {
        log.info("⚙️ 관제 시스템 동적 조회 구동 - 파라미터 상태: [{}], 시민ID: [{}]", status, citizenId);
        
        // 데이터 전처리 안전장치 가동
        String targetStatus = (status != null && !status.trim().isEmpty()) ? status.trim() : null;

        // 💡 복잡한 Dynamic Query 인프라 구축 전, 관제소 로딩 속도 최적화를 위한 조건별 분기 필터링 처리
        List<Report> reports;
        if (targetStatus != null && citizenId != null) {
            reports = reportRepository.findAllByStatusAndCitizenIdOrderByCreatedAtDesc(targetStatus, citizenId);
        } else if (targetStatus != null) {
            reports = reportRepository.findAllByStatusOrderByCreatedAtDesc(targetStatus);
        } else if (citizenId != null) {
            reports = reportRepository.findAllByCitizenIdOrderByCreatedAtDesc(citizenId);
        } else {
            reports = reportRepository.findAll();
        }

        // 하이버네이트 프록시 레이어를 완전 휘발시키고 순수 데이터 응답 객체(ReportResponse)로 패키징 매핑
        return reports.stream()
                .map(ReportResponse::new)
                .collect(Collectors.toList());
    }

    // 내 민원 목록 조회 (Read)
    public List<ReportResponse> getMyReports(Long citizenId) {
        log.info("⚙️ DB에서 시민 ID [{}]의 민원 목록을 최신순으로 조회합니다.", citizenId);
        return reportRepository.findAllByCitizenIdOrderByCreatedAtDesc(citizenId)
                .stream()
                .map(ReportResponse::new)
                .collect(Collectors.toList());
    }
}
