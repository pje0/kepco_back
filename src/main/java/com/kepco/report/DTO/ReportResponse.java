package com.kepco.report.DTO;

import com.kepco.report.entity.Report;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ReportResponse {
    private Long id;
    private String title;
    private String content;
    private String category;
    private String address;
    private String district;
    private String status;
    private LocalDateTime createdAt;
    
    // 조인이 필요한 추가 정보 (프론트엔드 에러 방지용)
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    private String assignedWorkerName;

    public ReportResponse(Report report) {
        this.id = report.getId();
        this.title = report.getTitle();
        this.content = report.getContent();
        this.address = report.getAddress();
        this.district = report.getDistrict();
        
        // 프론트엔드의 상태 라벨과 맞추기 위한 한글 변환
        switch (report.getStatus()) {
            case "in_progress": this.status = "처리중"; break;
            case "resolved": this.status = "처리완료"; break;
            default: this.status = "미처리"; break;
        }
        
        this.createdAt = report.getCreatedAt();
        
        // TODO: category, assignedWorkerName 등은 타 테이블 조인 로직 추가 후 변경 필요
        this.category = "분석 대기"; 
    }
}