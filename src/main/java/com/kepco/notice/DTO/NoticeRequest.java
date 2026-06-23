package com.kepco.notice.DTO;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class NoticeRequest {
    private String department;
    private String title;
    private String content;
    private Boolean isPinned;
    private LocalDateTime publishAt;
}