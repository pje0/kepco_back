// src/main/java/com/kepco/notice/repository/NoticeRepository.java
package com.kepco.notice.repository;

import com.kepco.notice.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {
    
    @Query("SELECT n FROM Notice n WHERE n.status = 'PUBLISHED' AND (n.publishAt IS NULL OR n.publishAt <= CURRENT_TIMESTAMP) ORDER BY n.isPinned DESC, n.createdAt DESC")
    List<Notice> findPublishedNotices();
    
    @Query("SELECT n FROM Notice n WHERE n.status = 'DRAFT' ORDER BY n.createdAt DESC")
    List<Notice> findDraftNotices();
}