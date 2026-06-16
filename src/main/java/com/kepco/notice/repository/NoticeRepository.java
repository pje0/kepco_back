package com.kepco.notice.repository;

import com.kepco.notice.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {
	// 상단 고정된 공지사항을 먼저 보여주고, 그 다음 최신순으로 정렬
    List<Notice> findAllByOrderByIsPinnedDescCreatedAtDesc();
}
