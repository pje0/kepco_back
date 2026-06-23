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
            case "in_progress": 
            case "working": // working 상태 대응 추가 (dispatch 연동 고려)
                this.status = "처리중"; 
                break;
            case "resolved": 
            case "completed": // completed 상태 대응 추가
                this.status = "처리완료"; 
                break;
            default: 
                this.status = "미처리"; 
                break;
        }
        
        this.createdAt = report.getCreatedAt();
        
        // 🚨 수정: 엔티티에서 AI 분석 카테고리를 꺼내오고, 아직 없으면 "분석 대기" 표출
        this.category = report.getAiCategory() != null ? report.getAiCategory() : "분석 대기"; 
    }
}