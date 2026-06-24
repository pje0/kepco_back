package com.kepco.work.service;

import com.kepco.auth.entity.RecoveryWorker;
import com.kepco.work.dto.WorkDispatchDto;
import com.kepco.work.repository.WorkDispatchRepository;
import com.kepco.work.repository.WorkWorkerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.kepco.dispatch.entity.Dispatch;
import java.time.LocalDateTime;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkService {

    private final WorkWorkerRepository workWorkerRepository;
    private final WorkDispatchRepository workDispatchRepository;

    @Transactional(readOnly = true)
    public List<WorkDispatchDto> getMyDispatches(String username) {
        RecoveryWorker worker = workWorkerRepository.findByUser_Username(username)
                .orElseThrow(() -> new IllegalArgumentException("출동요원 정보를 찾을 수 없습니다."));

        return workDispatchRepository
                .findByRecoveryWorker_IdOrderByAssignedAtDesc(worker.getId())
                .stream()
                .map(WorkDispatchDto::from)
                .toList();
    }
    
    @Transactional
    public void updateStatus(String username, Long dispatchId, String status, String workNote) {
        // 내 출동이 맞는지 확인 (남의 출동 못 바꾸게)
        RecoveryWorker worker = workWorkerRepository.findByUser_Username(username)
                .orElseThrow(() -> new IllegalArgumentException("출동요원 정보를 찾을 수 없습니다."));

        Dispatch d = workDispatchRepository.findById(dispatchId)
                .orElseThrow(() -> new IllegalArgumentException("출동 건을 찾을 수 없습니다."));

        if (!d.getRecoveryWorker().getId().equals(worker.getId())) {
            throw new IllegalArgumentException("본인에게 배정된 출동만 변경할 수 있습니다.");
        }

        d.setStatus(status);
        if ("IN_PROGRESS".equals(status)) {
            d.setArrivedAt(LocalDateTime.now());
        } else if ("RESOLVED".equals(status)) {
            d.setCompletedAt(LocalDateTime.now());
            if (workNote != null) d.setWorkNote(workNote);
        }
        workDispatchRepository.save(d);
    }
}