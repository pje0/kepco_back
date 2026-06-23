package com.kepco.complaint.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "complaint")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // auth 패키지의 User 엔티티와 강한 결합을 피하기 위해 외래키 대신 ID 직접 참조 방식을 사용합니다.
    @Column(name = "citizen_id", nullable = false)
    private Long citizenId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private String region;       // 시/도
    private String district;     // 시/군/구
    
    @Column(nullable = false)
    private String address;      // 발생 장소 상세 주소

    @Column(nullable = false, length = 20)
    private String status = "PENDING"; // PENDING, IN_PROGRESS, RESOLVED

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder
    public Complaint(Long citizenId, String title, String content, String region, String district, String address) {
        this.citizenId = citizenId;
        this.title = title;
        this.content = content;
        this.region = region;
        this.district = district;
        this.address = address;
    }

    // 비즈니스 로직: 관제사가 직원을 배정하거나 해결했을 때 상태 변경용 메서드
    public void updateStatus(String newStatus) {
        this.status = newStatus;
    }
}
