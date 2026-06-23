package com.kepco.auth.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicInsert;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.fasterxml.jackson.annotation.JsonIgnore; // 🚨 순환 참조 방어용 임포트 추가

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users") 
@Getter
@NoArgsConstructor
@DynamicInsert 
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", unique = true, nullable = false, length = 100)
    private String username; 

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(unique = true, length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false, length = 20)
    private String role; 

    @Column(name = "hired_at")
    private LocalDate hiredAt; 

    @Column(name = "emp_number", unique = true, length = 50)
    private String empNumber; 

    @Column(length = 100)
    private String department; 

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // =========================================================================
    // 🔗 JPA 1:1 양방향 관계 설정 및 Jackson 직렬화 무한 루프 차단벽 탑재
    // =========================================================================
    @JsonIgnore // 🚨 [치명적 무한루프 전면 차단] User를 직렬화할 때 자식 프로필로의 역참조를 전면 격리합니다.
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private RecoveryWorker recoveryWorker; 

    @Builder
    public User(String username, String password, String name, String email, String phone, String role, LocalDate hiredAt, String empNumber, String department) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = role != null ? role : "CITIZEN";
        this.hiredAt = hiredAt;
        this.empNumber = empNumber;
        this.department = department;
    }

    // =========================================================================
    // 🛠️ 비즈니스 전용 안전 변경 메서드 (무분별한 @Setter 방지)
    // =========================================================================
    public void changePassword(String encodedPassword) { this.password = encodedPassword; }
    public void changeName(String name) { this.name = name; }
    public void changeEmail(String email) { this.email = email; }
    public void changePhone(String phone) { this.phone = phone; }
    public void changeRole(String newRole) { this.role = newRole; }
    public void setHiredAt(LocalDate hiredAt) { this.hiredAt = hiredAt; }
    public void changeEmpNumber(String empNumber) { this.empNumber = empNumber; }
    public void changeDepartment(String department) { this.department = department; }

    public void createRecoveryWorkerProfile(String empNumber, String department, String assignedDistrict, String certificate, String grade) {
        this.empNumber = empNumber;
        this.department = department;
        this.recoveryWorker = RecoveryWorker.builder()
                .assignedDistrict(assignedDistrict)
                .certificate(certificate)
                .grade(grade)
                .user(this) 
                .build();
        this.hiredAt = LocalDate.now();
    }
}
