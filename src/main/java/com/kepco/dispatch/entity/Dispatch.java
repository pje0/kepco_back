package com.kepco.dispatch.entity;

import com.kepco.auth.entity.User;
import com.kepco.auth.entity.RecoveryWorker;
import com.kepco.report.entity.Report; // 💡 우리가 찾은 Report 패키지 경로 매핑!

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "dispatch")
@Data
@NoArgsConstructor
public class Dispatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 1. 민원 테이블과의 다대일(N:1) 조인 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Report complaint;

    // 2. 출동 대원 테이블과의 다대일(N:1) 조인 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private RecoveryWorker recoveryWorker;

    // 3. 관제사(users) 테이블과의 다대일(N:1) 조인 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispatcher_id", nullable = false)
    private User dispatcher;

    @Column(length = 20, nullable = false)
    private String status = "assigned"; // 기본값 'assigned' (배정완료)

    @Column(name = "assigned_at", updatable = false)
    private LocalDateTime assignedAt = LocalDateTime.now(); // 배정 시각

    @Column(name = "arrived_at")
    private LocalDateTime arrivedAt; // 현장 도착 시각

    @Column(name = "completed_at")
    private LocalDateTime completedAt; // 복구 완료 시각

    @Column(name = "work_note", columnDefinition = "TEXT")
    private String workNote; // 지시 사항 및 조치 내역
}
