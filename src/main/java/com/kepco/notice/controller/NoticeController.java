package com.kepco.notice.controller;

import com.kepco.notice.DTO.NoticeResponse;
import com.kepco.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    // 공지사항 목록 조회 API
    // 프론트엔드의 getNoticesAPI() 와 연결됩니다.
    @GetMapping
    public ResponseEntity<List<NoticeResponse>> getNotices() {
        List<NoticeResponse> response = noticeService.getAllNotices();
        return ResponseEntity.ok(response);
    }

    // 공지사항 상세 조회 API
    @GetMapping("/{id}")
    public ResponseEntity<NoticeResponse> getNoticeDetail(@PathVariable("id") Long id) {
        NoticeResponse response = noticeService.getNoticeDetail(id);
        return ResponseEntity.ok(response);
    }
}