// src/main/java/com/kepco/notice/controller/NoticeTemplateController.java (v1.0)
package com.kepco.notice.controller;

import com.kepco.notice.entity.NoticeTemplate;
import com.kepco.notice.repository.NoticeTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/notices/templates")
@RequiredArgsConstructor
public class NoticeTemplateController {

    private final NoticeTemplateRepository noticeTemplateRepository;

    @GetMapping
    public List<NoticeTemplate> getAllTemplates() {
        // [콘솔 로그] API 호출 여부 감시
        System.out.println("[NoticeTemplateController] 공지사항 고정 템플릿 목록 조회 요청 수신");
        
        List<NoticeTemplate> templates = noticeTemplateRepository.findAllByOrderByCreatedAtDesc();
        
        // [콘솔 로그] 반환 데이터 검증
        System.out.println("[NoticeTemplateController] 조회 완료된 템플릿 총 개수: " + templates.size());
        return templates;
    }
}