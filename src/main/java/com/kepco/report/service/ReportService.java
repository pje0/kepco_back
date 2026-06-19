package com.kepco.report.service;

import com.kepco.report.DTO.ReportCreateRequest;
import com.kepco.report.DTO.ReportResponse;
import com.kepco.report.entity.Report;
import com.kepco.report.repository.ReportRepository;
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
        return savedId;
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