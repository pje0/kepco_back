// src/main/java/com/kepco/notice/DTO/NoticeRequest.java
package com.kepco.notice.DTO;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NoticeRequest {
    private String title;
    private String content;
    private String department;
    private Boolean isPinned;
    private LocalDateTime publishAt;
    private String status; // 'published' 또는 'draft' 수신용
}