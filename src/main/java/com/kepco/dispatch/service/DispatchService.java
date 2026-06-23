package com.kepco.dispatch.service;

import com.kepco.auth.entity.RecoveryWorker;
import com.kepco.auth.entity.User;
import com.kepco.auth.repository.UserRepository;
import com.kepco.dispatch.dto.*;
import com.kepco.dispatch.entity.Dispatch;
import com.kepco.dispatch.repository.DispatchRepository;
import com.kepco.dispatch.repository.RecoveryWorkerRepository;
import com.kepco.report.repository.ReportRepository;
import com.kepco.report.entity.Report;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
     */
    public DispatchDashboardDto getDashboardData() {

        log.info("@# DispatchService - 대시보드 데이터 조회 시작");

        DispatchDashboardDto dto = new DispatchDashboardDto();

        dto.setPendingCount(reportRepository.countByStatus("pending"));

        long activeCount =
                dispatchRepository.countByStatus("assigned")
                + dispatchRepository.countByStatus("dispatched");

        dto.setActiveCount(activeCount);

        dto.setCompletedCount(
                dispatchRepository.countByStatus("completed")
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
                            // 신고 정보
                            if (dispatch.getComplaint() != null) {
                                item.setComplaintTitle(
                                        dispatch.getComplaint().getTitle()
                                );
                            } else {
                                item.setComplaintTitle("알 수 없는 신고 건");
                            }
                            // 작업자 정보
                            if (dispatch.getRecoveryWorker() != null
                                    && dispatch.getRecoveryWorker().getUser() != null) {

                                item.setWorkerName(
                                        dispatch.getRecoveryWorker()
                                                .getUser()
                                                .getName()
                                );
                            } else {
                                item.setWorkerName("미지정 요원");
                            }
                            item.setAssignedAt(dispatch.getAssignedAt());

                            item.setWorkNote(
                                    dispatch.getWorkNote() != null
                                            ? dispatch.getWorkNote()
                                            : ""
                            );
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
        return reportRepository.findByStatus("pending")
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
                    dto.setName(
                            w.getUser() != null
                                    ? w.getUser().getName()
                                    : "이름 없음"
                    );
                    dto.setGrade(w.getGrade());
                    dto.setCertificate(w.getCertificate());

                    return dto;
                })
                .collect(Collectors.toList());
    }
    /**
     * 4. 신규 현장 파견 지시
     */
    @Transactional
    public void createDispatch(
            DispatchCreateRequestDto requestDto,
            String dispatcherUsername
    ) {
        log.info("@# 신규 파견 생성 시작");

        Report report = reportRepository.findById(requestDto.getComplaintId())
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 신고 건입니다.")
                );
        RecoveryWorker worker =
                recoveryWorkerRepository.findById(requestDto.getWorkerId())
                        .orElseThrow(() ->
                                new IllegalArgumentException("존재하지 않는 현장 요원입니다.")
                        );
        User dispatcher = userRepository.findByUsername(dispatcherUsername)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 관제사 계정입니다.")
                );
        // 신고 상태 변경
        reportRepository.updateStatus(
                report.getId(),
                "assigned"
        );
        // 작업자 상태 변경
        recoveryWorkerRepository.updateWorkStatus(
                worker.getId(),
                "UNAVAILABLE"
        );
        log.info("@# 신고 상태 -> assigned");
        log.info("@# 작업자 상태 -> UNAVAILABLE");
        // Dispatch 생성
        Dispatch dispatch = new Dispatch();

        dispatch.setComplaint(report);
        dispatch.setRecoveryWorker(worker);
        dispatch.setDispatcher(dispatcher);
        dispatch.setStatus("assigned");
        dispatch.setAssignedAt(LocalDateTime.now());
        dispatch.setWorkNote(requestDto.getWorkNote());

        dispatchRepository.save(dispatch);
        log.info("@# 신규 파견 저장 완료");
    }
    /**
     * 5. 현장 복구 완료 처리
     */
    @Transactional
    public void completeDispatch(Long dispatchId, String workNote) {

        log.info("@# 파견 완료 처리 시작 - dispatchId={}", dispatchId);

        Dispatch dispatch = dispatchRepository.findById(dispatchId)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 파견 이력입니다.")
                );
        dispatch.setStatus("completed");
        dispatch.setCompletedAt(LocalDateTime.now());

        if (workNote != null && !workNote.trim().isEmpty()) {
            dispatch.setWorkNote(workNote);
        }

        // 신고 상태 완료 처리
        if (dispatch.getComplaint() != null) {

            reportRepository.updateStatus(
                    dispatch.getComplaint().getId(),
                    "completed"
            );
            log.info("@# 신고 상태 -> completed");
        }
        // 작업자 복귀 처리
        if (dispatch.getRecoveryWorker() != null) {

            recoveryWorkerRepository.updateWorkStatus(
                    dispatch.getRecoveryWorker().getId(),
                    "AVAILABLE"
            );
            log.info("@# 작업자 상태 -> AVAILABLE");
        }
        log.info("@# 파견 완료 처리 종료");
    }
}