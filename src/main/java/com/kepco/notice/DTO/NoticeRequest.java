package com.kepco.notice.DTO;

import lombok.Data;

@Data
public class NoticeRequest {
    private String department;
    private String title;
    private String content;
    private Boolean isPinned;
}