package com.kepco.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(length = 100)
    private String email;

    // 한전 시스템 특성상 권한 관리가 중요하므로 ROLE 추가 (기본값 ROLE_USER)
    @Column(nullable = false, length = 20)
    private String role = "ROLE_USER"; 

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}