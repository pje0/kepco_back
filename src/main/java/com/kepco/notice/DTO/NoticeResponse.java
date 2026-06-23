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
    
    // 🚨 프론트엔드에서 예약 날짜 유무를 별도로 확인할 수 있도록 필드 추가
    private LocalDateTime publishAt; 
    
    // 🚨 프론트엔드가 요구하는 두 가지 상태값을 모두 제공해야 화면이 깨지지 않습니다.
    private Boolean isPinned;  // 목록 페이지용
    private String priority;   // 상세 페이지용

    public NoticeResponse(Notice notice) {
        // 기본 정보 매핑
        this.id = notice.getId();
        this.title = notice.getTitle();
        this.content = notice.getContent();
        this.views = notice.getViews();
        
        // 🚨 핵심: 예약 시간(publishAt)이 존재할 경우, 화면에 노출될 작성일(createdAt)을 예약 시간으로 강제 교체
        this.publishAt = notice.getPublishAt();
        this.createdAt = notice.getPublishAt() != null ? notice.getPublishAt() : notice.getCreatedAt();
        
        // 작성자 및 부서 정보 매핑
        this.department = notice.getDepartment(); 
        this.author = "관리자"; 
        
        // 🚨 프론트엔드 상태값 필드 매핑
        this.isPinned = notice.getIsPinned(); 
        this.priority = notice.getIsPinned() ? "high" : "normal";

        // [콘솔 로그] 데이터 매핑 결과 확인 (작성일이 예약일로 정확히 덮어씌워졌는지 서버 로그로 검증)
        System.out.println("[NoticeResponse v1.6] 글 ID: " + this.id + " | 최종 화면 노출용 작성일(createdAt) 세팅 완료: " + this.createdAt);
    }
}