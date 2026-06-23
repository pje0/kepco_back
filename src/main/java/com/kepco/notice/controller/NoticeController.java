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
    
    // 공지사항 작성
    @PostMapping
    public ResponseEntity<Long> createNotice(@RequestBody com.kepco.notice.DTO.NoticeRequest dto) {
        Long writerId = 6L; // (총괄관리자 기본값) 추후 SecurityContext에서 가져오도록 변경 가능
        return ResponseEntity.ok(noticeService.createNotice(writerId, dto));
    }

    // 공지사항 수정
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateNotice(@PathVariable("id") Long id, @RequestBody com.kepco.notice.DTO.NoticeRequest dto) {
        noticeService.updateNotice(id, dto);
        return ResponseEntity.ok().build();
    }

    // 공지사항 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotice(@PathVariable("id") Long id) {
        noticeService.deleteNotice(id);
        return ResponseEntity.ok().build();
    }
    
    // 임시저장 목록 조회 API
    @GetMapping("/drafts")
    public ResponseEntity<List<NoticeResponse>> getDraftNotices() {
        return ResponseEntity.ok(noticeService.getDraftNotices());
    }
}