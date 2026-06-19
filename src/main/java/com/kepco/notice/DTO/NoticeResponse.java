package com.kepco.notice.DTO;

import com.kepco.notice.entity.Notice;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class NoticeResponse {
    private Long id;
    private String title;
    private String content;
    private String author;     
    private String department; 
    private Integer views;
    private LocalDateTime createdAt;
    
    // 🚨 프론트엔드가 요구하는 두 가지 상태값을 모두 제공해야 화면이 깨지지 않습니다.
    private Boolean isPinned;  // 목록 페이지용
    private String priority;   // 상세 페이지용

    public NoticeResponse(Notice notice) {
        this.id = notice.getId();
        this.title = notice.getTitle();
        this.content = notice.getContent();
        this.views = notice.getViews();
        this.createdAt = notice.getCreatedAt();
        
        this.department = notice.getDepartment(); 
        this.author = "관리자"; 
        
        // 🚨 필드 매핑 추가
        this.isPinned = notice.getIsPinned(); 
        this.priority = notice.getIsPinned() ? "high" : "normal";
    }
}