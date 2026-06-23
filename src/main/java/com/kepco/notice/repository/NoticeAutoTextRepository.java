package com.kepco.notice.repository;

import com.kepco.notice.entity.NoticeAutoText;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NoticeAutoTextRepository extends JpaRepository<NoticeAutoText, Long> {
}