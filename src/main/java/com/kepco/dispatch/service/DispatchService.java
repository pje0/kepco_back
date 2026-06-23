package com.kepco.dispatch.service;

import com.kepco.auth.entity.RecoveryWorker;
import com.kepco.auth.entity.User;
import com.kepco.auth.repository.UserRepository;
import com.kepco.dispatch.dto.*;
import com.kepco.dispatch.dto.DispatchDashboardDto.DispatchItem;
import com.kepco.dispatch.entity.Dispatch;
import com.kepco.dispatch.repository.DispatchRepository;
import com.kepco.dispatch.repository.RecoveryWorkerRepository;
import com.kepco.report.repository.ReportRepository;
import com.kepco.report.entity.Report;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class DispatchService {

    private final DispatchRepository dispatchRepository;
    private final ReportRepository reportRepository;
    private final RecoveryWorkerRepository recoveryWorkerRepository;
    private final UserRepository userRepository;

    /**
     * 1. 파견 관리 대시보드 데이터 및 카운트 전체 조회
     * - ⚡ [대문자 개혁 완결]: DB 마이그레이션 정합성을 일치시켜 상단 KPI 카운트 완전 정상 가동
     */
    public DispatchDashboardDto getDashboardData() {
        log.info("@# DispatchService - 대시보드 데이터 조회 시작");
        DispatchDashboardDto dto = new DispatchDashboardDto();

        // 💡 대문자 PENDING 조건으로 변경하여 KPI 카운트 단선 원천 복구
        dto.setPendingCount(reportRepository.countByStatus("PENDING"));

        long activeCount =
                dispatchRepository.countByStatus("ASSIGNED")
                + dispatchRepository.countByStatus("DISPATCHED")
                + dispatchRepository.countByStatus("IN_PROGRESS"); 

        dto.setActiveCount(activeCount);

        // 💡 대문자 RESOLVED 조건 동기화 완료
        dto.setCompletedCount(
                dispatchRepository.countByStatus("RESOLVED")
        );

        dto.setAvailableWorkers(
                recoveryWorkerRepository.countByWorkStatus("AVAILABLE")
        );

        List<DispatchDashboardDto.DispatchItem> itemList =
                dispatchRepository.findAllByOrderByIdDesc()
                        .stream()
                        .map(dispatch -> {
                            DispatchDashboardDto.DispatchItem item =
                                    new DispatchDashboardDto.DispatchItem();

                            item.setDispatchId(dispatch.getId());
                            if (dispatch.getComplaint() != null) {
                                item.setComplaintTitle(dispatch.getComplaint().getTitle());
                            } else {
                                item.setComplaintTitle("알 수 없는 신고 건");
                            }
                            if (dispatch.getRecoveryWorker() != null
                                    && dispatch.getRecoveryWorker().getUser() != null) {
                                item.setWorkerName(dispatch.getRecoveryWorker().getUser().getName());
                            } else {
                                item.setWorkerName("미지정 요원");
                            }
                            item.setAssignedAt(dispatch.getAssignedAt());
                            item.setWorkNote(dispatch.getWorkNote() != null ? dispatch.getWorkNote() : "");
                            item.setStatus(dispatch.getStatus());
                            return item;
                        })
                        .collect(Collectors.toList());

        dto.setDispatchList(itemList);
        log.info("@# DispatchService - 대시보드 데이터 조회 완료");
        return dto;
    }

    /**
     * 2. 미배정 대기 신고 건 목록 조회
     */
    public List<PendingComplaintDto> getPendingComplaints() {
        log.info("@# 미배정 신고 목록 조회");
        // 💡 대문자 PENDING 매싱 개통
        return reportRepository.findByStatus("PENDING")
                .stream()
                .map(r -> {
                    PendingComplaintDto dto = new PendingComplaintDto();
                    dto.setId(r.getId());
                    dto.setTitle(r.getTitle());
                    dto.setRegion(r.getRegion());
                    dto.setDistrict(r.getDistrict());
                    dto.setAddress(r.getAddress());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * 3. 현재 출동 가능한 가용 요원 목록 조회
     */
    public List<AvailableWorkerDto> getAvailableWorkers() {
        log.info("@# 가용 작업자 목록 조회");
        return recoveryWorkerRepository.findByWorkStatus("AVAILABLE")
                .stream()
                .map(w -> {
                    AvailableWorkerDto dto = new AvailableWorkerDto();
                    dto.setId(w.getId());
                    dto.setName(w.getUser() != null ? w.getUser().getName() : "이름 없음");
                    dto.setGrade(w.getGrade());
                    dto.setCertificate(w.getCertificate());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * 4. 신규 현장 파견 지시
     * - ⚡ [대문자 전면 개혁]: 새로운 파견 트랜잭션 적재 및 업데이트 시 상태 코드를 무조건 대문자로 빌딩
     */
    @Transactional
    public void createDispatch(DispatchCreateRequestDto requestDto, String dispatcherUsername) {
        log.info("@# 신규 파견 생성 시작");

        Report report = reportRepository.findById(requestDto.getComplaintId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 신고 건입니다."));
        RecoveryWorker worker = recoveryWorkerRepository.findById(requestDto.getWorkerId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 현장 요원입니다."));
        User dispatcher = userRepository.findByUsername(dispatcherUsername)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 관제사 계정입니다."));

        // 💡 대문자 ASSIGNED 로 보정
        reportRepository.updateStatus(report.getId(), "ASSIGNED");
        recoveryWorkerRepository.updateWorkStatus(worker.getId(), "UNAVAILABLE");
        
        log.info("@# 신고 상태 -> ASSIGNED");
        log.info("@# 작업자 상태 -> UNAVAILABLE");

        Dispatch dispatch = new Dispatch();
        dispatch.setComplaint(report);
        dispatch.setRecoveryWorker(worker);
        dispatch.setDispatcher(dispatcher);
        // 💡 엔티티 내부 적재 기본 문자열 대문자 개혁 반영
        dispatch.setStatus("ASSIGNED");
        dispatch.setAssignedAt(LocalDateTime.now());
        dispatch.setWorkNote(requestDto.getWorkNote());

        dispatchRepository.save(dispatch);
        log.info("@# 신규 파견 저장 완료");
    }

    /**
     * 5. 현장 복구 완료 처리 (명세 규칙 상태 고정 버전)
     * - ⚡ [대문자 전면 개혁]: 트랜잭션 종료 시 최종 상태값을 대문자로 강제 영구 적재
     */
    @Transactional
    public void completeDispatch(Long dispatchId, String workNote) {
        log.info("@# 파견 완료 처리 시작 - dispatchId={}", dispatchId);

        Dispatch dispatch = dispatchRepository.findById(dispatchId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 파견 이력입니다."));
        
        // 🚨 명세서 물리 규칙 반영 및 대문자 강제 교정: resolved -> RESOLVED
        dispatch.setStatus("RESOLVED");
        dispatch.setCompletedAt(LocalDateTime.now());

        if (workNote != null && !workNote.trim().isEmpty()) {
            dispatch.setWorkNote(workNote);
        }

        // 🚨 명세서 물리 규칙 반영 및 대문자 강제 교정: 민원 테이블 완료 상태도 RESOLVED로 변경
        if (dispatch.getComplaint() != null) {
            reportRepository.updateStatus(dispatch.getComplaint().getId(), "RESOLVED");
            log.info("@# 신고 상태 -> RESOLVED");
        }
        
        if (dispatch.getRecoveryWorker() != null) {
            recoveryWorkerRepository.updateWorkStatus(dispatch.getRecoveryWorker().getId(), "AVAILABLE");
            log.info("@# 작업자 복귀 처리 -> AVAILABLE");
        }
    }

    /**
     * ⚡ [교정 완료] PostgreSQL 부분 인덱스 최적화 및 하이버네이트 프록시 원천 박멸 이력 검색 엔진
     * - 💡 [대문자 전면 개혁]: totalElements: 0건 누락 버그를 격파하기 위해 
     *   조회 대상을 소문자 'resolved'가 아닌 공인 규격 대문자 'RESOLVED' 조건으로 강제 조율!
     */
    public Page<DispatchItem> getDispatchHistory(HistorySearchRequestDto dto, Pageable pageable) {
        log.info("@# 과거 이력 고속 복합 검색 엔진 가동");

        // 1. 진입 시 달력 날짜 미선택 시 기본 7일 구간 닫기 공식 가동
        LocalDateTime startDateTime = (dto.getStartDate() != null) 
                ? dto.getStartDate().atStartOfDay() 
                : LocalDateTime.now().minusDays(7).with(LocalTime.MIN);
                
        LocalDateTime endDateTime = (dto.getEndDate() != null) 
                ? dto.getEndDate().atTime(LocalTime.MAX) 
                : LocalDateTime.now().with(LocalTime.MAX);

        // 2. 🚀 [근본 해결]: dispatchRepository에 대문자 'RESOLVED' 파라미터를 정확히 실어 데이터베이스 스캔 명령 하달
        Page<Dispatch> entityPage = dispatchRepository.findAllByStatusAndCompletedAtBetween(
                "RESOLVED", 
                startDateTime, 
                endDateTime, 
                dto,
                pageable
        );

        // 3. 🎯 [핵심 교정]: 프록시 껍데기(ByteBuddy)가 섞여있는 엔티티를 순수 텍스트 DTO로 원샷 변환
        return entityPage.map(dispatch -> {
            DispatchItem item = new DispatchItem();

            item.setDispatchId(dispatch.getId());
            item.setAssignedAt(dispatch.getAssignedAt());
            item.setStatus(dispatch.getStatus());
            item.setWorkNote(dispatch.getWorkNote() != null ? dispatch.getWorkNote() : "");

            // 🔗 연관된 민원(Complaint) 정보에서 순수 데이터만 바인딩 (프록시 원천 제거)
            if (dispatch.getComplaint() != null) {
                item.setComplaintTitle(dispatch.getComplaint().getTitle());
            } else {
                item.setComplaintTitle("알 수 없는 신고 건");
            }

            // 🔗 연관된 현장 요원(RecoveryWorker -> User) 정보에서 순수 이름만 추출 (프록시 원천 제거)
            if (dispatch.getRecoveryWorker() != null && dispatch.getRecoveryWorker().getUser() != null) {
                item.setWorkerName(dispatch.getRecoveryWorker().getUser().getName());
            } else {
                item.setWorkerName("미지정 요원");
            }

            return item;
        });
    }
}