package com.kepco.report.service;

import com.kepco.report.DTO.ReportCreateRequest;
import com.kepco.report.DTO.ReportResponse;
import com.kepco.report.entity.Report;
import com.kepco.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;

    // 민원 접수 (Create)
    @Transactional
    public Long createReport(ReportCreateRequest request) {
        Report report = Report.builder()
                .citizenId(request.getCitizenId())
                .title(request.getTitle())
                .content(request.getContent())
                .region("부산광역시") // 기본값 세팅
                .district(request.getDistrict())
                .address(request.getAddress())
                .build();

        return reportRepository.save(report).getId();
    }

    // 내 민원 목록 조회 (Read)
    public List<ReportResponse> getMyReports(Long citizenId) {
        return reportRepository.findAllByCitizenIdOrderByCreatedAtDesc(citizenId)
                .stream()
                .map(ReportResponse::new)
                .collect(Collectors.toList());
    }
}