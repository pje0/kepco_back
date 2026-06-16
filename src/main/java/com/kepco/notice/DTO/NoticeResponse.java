package com.kepco.notice.DTO;

import com.kepco.notice.entity.Notice;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class NoticeResponse {
    private Long id;
    private String title;
    private String content;
    private String author;     // 프론트엔드 요구사항 (원래는 User 테이블 조인 필요)
    private String priority;   // 프론트엔드 요구사항 (high, normal, low)
    private Integer views;
    private LocalDateTime createdAt;

    public NoticeResponse(Notice notice) {
        this.id = notice.getId();
        this.title = notice.getTitle();
        this.content = notice.getContent();
        this.views = notice.getViews();
        this.createdAt = notice.getCreatedAt();
        
        // TODO: 실제 구현 시에는 writerId를 이용해 User 테이블에서 이름을 가져와야 합니다.
        this.author = "관리자(" + notice.getWriterId() + ")"; 
        
        // DB의 is_pinned를 프론트엔드의 priority 포맷으로 변환
        this.priority = notice.getIsPinned() ? "high" : "normal";
    }
}