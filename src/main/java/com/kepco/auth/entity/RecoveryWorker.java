package com.kepco.auth.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "recovery_worker") // ⭕ 최종 테이블명 매칭
@Getter
@NoArgsConstructor
@DynamicInsert // 기본값인 'AVAILABLE'이 안전하게 활성화되도록 설정
public class RecoveryWorker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔗 1:1 관계 설정 - 부모 users 테이블의 id를 users_id(FK) 컬럼으로 참조
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "users_id", nullable = false, unique = true) // 🔴 [교정 완료] 실제 DB의 스펙과 완벽 싱크 매칭 ('user_id' -> 'users_id')
    private User user;

    @Column(name = "assigned_district", nullable = false, length = 50)
    private String assignedDistrict; // 담당 구역 (OpenAI 지역 매칭용)

    @Column(length = 255)
    private String certificate; // 보유 자격증 목록 (OpenAI 분석용 소스)

    @Column(length = 20)
    private String grade; // 직급 (JUNIOR, SENIOR, MASTER 등)

    @Column(name = "work_status", nullable = false, length = 20)
    private String workStatus = "AVAILABLE"; // 직원 상태 (AVAILABLE, DISPATCHED, UNAVAILABLE)

    @Column(name = "resigned_at")
    private LocalDateTime resignedAt; // 퇴직 일시

    @UpdateTimestamp // 데이터가 수정될 때마다 현재 시간으로 자동 리로드
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public RecoveryWorker(User user, String assignedDistrict, 
                          String certificate, String grade, String workStatus) {
        this.user = user;
        this.assignedDistrict = assignedDistrict;
        this.certificate = certificate;
        this.grade = grade;
        if (workStatus != null) {
            this.workStatus = workStatus;
        }
    }

    // =========================================================================
    // 🛠️ 인사팀 전용 OpenAI 스펙 실시간 수정 메서드 (변경 감지 연동)
    // =========================================================================
    public void updateWorkerSpecs(String assignedDistrict, String certificate, String grade) {
        if (assignedDistrict != null) this.assignedDistrict = assignedDistrict;
        if (certificate != null) this.certificate = certificate;
        if (grade != null) this.grade = grade;
    }

    // 파견 지령 수신 시 상태 변경을 위한 보조 메서드
    public void changeWorkStatus(String status) {
        this.workStatus = status;
    }

    // 퇴사 발령 시 기록용 메서드
    public void setResignedAt(LocalDateTime resignedAt) {
        this.resignedAt = resignedAt;
    }
}
