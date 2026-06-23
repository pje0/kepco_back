package com.kepco.notice.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notice_auto_text")
@Getter
@NoArgsConstructor
public class NoticeAutoText {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String shortcut;

    @Column(nullable = false)
    private String replacement;

    @Builder
    public NoticeAutoText(String shortcut, String replacement) {
        this.shortcut = shortcut;
        this.replacement = replacement;
    }
}