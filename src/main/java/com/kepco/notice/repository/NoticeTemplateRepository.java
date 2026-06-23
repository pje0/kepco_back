// src/main/java/com/kepco/notice/repository/NoticeTemplateRepository.java (v1.0)
package com.kepco.notice.repository;

import com.kepco.notice.entity.NoticeTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NoticeTemplateRepository extends JpaRepository<NoticeTemplate, Long> {
    // 최신 순으로 템플릿 목록 전체 조회
    List<NoticeTemplate> findAllByOrderByCreatedAtDesc();
}