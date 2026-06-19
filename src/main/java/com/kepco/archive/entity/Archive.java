package com.kepco.archive.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "archive")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Archive {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "writer_id", nullable = false)
    private Long writerId;

    private String department;
    private String category;
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    private Integer views;

    @Column(name = "download_count")
    private Integer downloadCount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "archive", fetch = FetchType.LAZY)
    private List<ArchiveAttachment> attachments = new ArrayList<>();
}