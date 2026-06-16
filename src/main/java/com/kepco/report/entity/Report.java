package com.kepco.report.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "complaint") // 실제 DB 테이블 연결
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "citizen_id", nullable = false)
    private Long citizenId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(length = 50)
    private String region;

    @Column(length = 50)
    private String district;

    @Column(length = 300, nullable = false)
    private String address;

    @Column(length = 20)
    private String status = "pending";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Report(Long citizenId, String title, String content, String region, String district, String address) {
        this.citizenId = citizenId;
        this.title = title;
        this.content = content;
        this.region = region;
        this.district = district;
        this.address = address;
        this.status = "pending"; // 초기 상태는 미처리(pending)로 고정
    }
}