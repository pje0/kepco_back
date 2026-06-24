package com.kepco.work.dto;

import com.kepco.dispatch.entity.Dispatch;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class WorkDispatchDto {
    private Long id;
    private String status;            // ASSIGNED, IN_PROGRESS, RESOLVED

    // 민원(현장) 정보
    private String complaintTitle;    // 민원 제목
    private String complaintAddress;  // 현장 주소
    private String aiCategory;        // AI 분류 카테고리
    private String aiPriority;        // AI 우선순위

    // 출동 시각 정보
    private LocalDateTime assignedAt;
    private LocalDateTime arrivedAt;
    private LocalDateTime completedAt;
    private String workNote;

    public static WorkDispatchDto from(Dispatch d) {
        var c = d.getComplaint();
        return WorkDispatchDto.builder()
                .id(d.getId())
                .status(d.getStatus())
                .complaintTitle(c != null ? c.getTitle() : null)
                .complaintAddress(c != null ? c.getAddress() : null)
                .aiCategory(c != null ? c.getAiCategory() : null)
                .aiPriority(c != null ? c.getAiPriority() : null)
                .assignedAt(d.getAssignedAt())
                .arrivedAt(d.getArrivedAt())
                .completedAt(d.getCompletedAt())
                .workNote(d.getWorkNote())
                .build();
    }
}