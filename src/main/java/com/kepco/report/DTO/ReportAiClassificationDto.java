package com.kepco.report.DTO;

import lombok.Data;

@Data
public class ReportAiClassificationDto  {
    // 🚨 우리가 DB 스펙 및 검색 필터로 고정한 카멜케이스 네이밍 완벽 일치
    private String aiCategory;  // 정전, 계량기 고장, 변압기 이상, 전선 단선 등
    private String aiPriority;  // CRITICAL, MAJOR, MINOR (심각도 코드값 고정)
}
