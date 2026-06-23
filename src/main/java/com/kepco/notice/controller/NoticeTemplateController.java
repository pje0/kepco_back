// src/main/java/com/kepco/notice/controller/NoticeTemplateController.java (v1.2)
package com.kepco.notice.controller;

import com.kepco.notice.entity.NoticeTemplate;
import com.kepco.notice.repository.NoticeTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notices/templates")
@RequiredArgsConstructor
public class NoticeTemplateController {

    private final NoticeTemplateRepository noticeTemplateRepository;

    @GetMapping
    public List<NoticeTemplate> getAllTemplates() {
        System.out.println("[NoticeTemplateController v1.2] 공지사항 고정 템플릿 목록 조회 요청 수신");
        List<NoticeTemplate> templates = noticeTemplateRepository.findAllByOrderByCreatedAtDesc();
        System.out.println("[NoticeTemplateController v1.2] 조회 완료된 템플릿 총 개수: " + templates.size());
        return templates;
    }

    @PostMapping
    public NoticeTemplate createTemplate(@RequestBody Map<String, String> body) {
        System.out.println("[NoticeTemplateController v1.2] 신규 고정 템플릿 등록 요청 수신");
        System.out.println("[NoticeTemplateController v1.2] 요청 페이로드 제목 데이터: " + body.get("title"));
        
        NoticeTemplate template = NoticeTemplate.builder()
                .title(body.get("title"))
                .content(body.get("content"))
                .department(body.get("department"))
                .build();
                
        NoticeTemplate savedTemplate = noticeTemplateRepository.save(template);
        System.out.println("[NoticeTemplateController v1.2] 고정 템플릿 등록 완료 - 엔티티 ID: " + savedTemplate.getId());
        return savedTemplate;
    }

    // 🚨 신규 추가: 템플릿 삭제(DELETE) 엔드포인트 매핑
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable("id") Long id) {
        System.out.println("[NoticeTemplateController v1.2] 고정 템플릿 삭제 요청 수신 - 대상 템플릿 ID: " + id);
        
        noticeTemplateRepository.deleteById(id);
        
        System.out.println("[NoticeTemplateController v1.2] 고정 템플릿 삭제 정상 처리 완료");
        return ResponseEntity.ok().build();
    }
}