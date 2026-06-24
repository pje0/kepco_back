// src/main/java/com/kepco/notice/entity/Notice.java
package com.kepco.notice.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notice")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "writer_id", nullable = false)
    private Long writerId;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "views", nullable = false)
    private Integer views = 0;

    @Column(name = "is_pinned", nullable = false)
    private Boolean isPinned = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "publish_at")
    private LocalDateTime publishAt;
    
    // 🚨 [수정] DB의 대문자 규격에 맞춰 기본값을 'PUBLISHED'로 세팅
    @Column(name = "status", length = 20)
    private String status = "PUBLISHED";
    
    @Builder
    public Notice(Long writerId, String title, String content, String department, Boolean isPinned, LocalDateTime publishAt, String status) {
        this.writerId = writerId;
        this.title = title;
        this.content = content;
        this.department = department;
        this.isPinned = isPinned != null ? isPinned : false;
        this.publishAt = publishAt;
        // 🚨 프론트에서 소문자가 넘어오더라도 강제로 대문자로 변환하여 저장
        this.status = status != null ? status.toUpperCase() : "PUBLISHED";
        this.views = 0;
    }

    public void incrementViews() {
        this.views++;
    }
    
    public void update(String title, String content, String department, Boolean isPinned, LocalDateTime publishAt, String status) {
        this.title = title;
        this.content = content;
        this.department = department;
        this.isPinned = isPinned != null ? isPinned : false;
        this.publishAt = publishAt;
        // 🚨 강제 대문자 변환
        this.status = status != null ? status.toUpperCase() : "PUBLISHED";
    }
}