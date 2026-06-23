package com.kepco.notice.service;

import com.kepco.notice.DTO.NoticeRequest;
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

    // 전체 공지사항 목록 조회 (상단 고정 우선, 최신순 정렬 + 🚨 예약 발행 필터링 적용)
    public List<NoticeResponse> getAllNotices() {
        // [디버깅용 콘솔 로그] 예약 필터링 쿼리 호출 시작
        System.out.println("[NoticeService] 발행 처리된 공지사항 목록 조회를 시작합니다."); 
        
        // 🚨 무조건 전부 가져오던 메서드 대신, 예약 시간이 검증된 커스텀 쿼리를 호출합니다.
        List<Notice> notices = noticeRepository.findPublishedNotices();
        
        // [디버깅용 콘솔 로그] 필터링 후 반환되는 데이터 개수 확인
        System.out.println("[NoticeService] 클라이언트로 반환할 공지사항 개수: " + notices.size()); 
        
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
    
    @Transactional
    public Long createNotice(Long writerId, NoticeRequest dto) {
        Notice notice = Notice.builder()
                .writerId(writerId)
                .department(dto.getDepartment())
                .title(dto.getTitle())
                .content(dto.getContent())
                .isPinned(dto.getIsPinned())
                .publishAt(dto.getPublishAt())
                .build();
        return noticeRepository.save(notice).getId();
    }

    @Transactional
    public void updateNotice(Long id, NoticeRequest dto) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 공지사항이 없습니다."));
        notice.update(dto.getTitle(), dto.getContent(), dto.getDepartment(), dto.getIsPinned(), dto.getPublishAt());
    }

    @Transactional
    public void deleteNotice(Long id) {
        noticeRepository.deleteById(id);
    }
}