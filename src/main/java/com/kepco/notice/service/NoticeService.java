package com.kepco.notice.service;

import com.kepco.notice.DTO.NoticeResponse;
import com.kepco.notice.entity.Notice;
import com.kepco.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;

    // 전체 공지사항 목록 조회 (상단 고정 우선, 최신순 정렬)
    public List<NoticeResponse> getAllNotices() {
        List<Notice> notices = noticeRepository.findAllByOrderByIsPinnedDescCreatedAtDesc();
        return notices.stream()
                .map(NoticeResponse::new)
                .collect(Collectors.toList());
    }

    // 단일 공지사항 조회 (조회수 증가 포함)
    @Transactional
    public NoticeResponse getNoticeDetail(Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 공지사항이 존재하지 않습니다. id=" + id));
        
        notice.incrementViews(); // 변경감지(Dirty Checking)로 자동 UPDATE 쿼리 발생
        return new NoticeResponse(notice);
    }
}