package com.kepco.auth.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicInsert;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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
@Table(name = "users") // ⭕ 최종 테이블명 'users' 완벽 매칭
@Getter
@NoArgsConstructor
@DynamicInsert // null인 필드는 insert 문에서 제외하여 DB DEFAULT 값(CITIZEN, NOW()) 활성화
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", unique = true, nullable = false, length = 100)
    private String username; // ⭕ DB 컬럼: username

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(unique = true, length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false, length = 20)
    private String role; // 접두사를 뺀 스키마 규격 반영 (CITIZEN, WORKER, DISPATCHER, HR, ADMIN)

    @Column(name = "hired_at")
    private LocalDate hiredAt; // ⭕ 직원 입사 일자 매칭 (일반 시민은 null)

    @Column(name = "emp_number", unique = true, length = 50)
    private String empNumber; // 사번

    @Column(length = 100)
    private String department; // 소속 지사 및 부서

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // =========================================================================
    // 🔗 JPA 1:1 양방향 관계 설정 (자식 테이블 recovery_worker와 유기적 연결)
    // =========================================================================
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private RecoveryWorker recoveryWorker; // 💡 자식 객체 바인딩 (주의: 다음 스텝에서 생성 예정)

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
    
    // 민원인/사원 공통 정보 수정 메서드
    public void changePassword(String encodedPassword) { this.password = encodedPassword; }
    public void changeName(String name) { this.name = name; }
    public void changeEmail(String email) { this.email = email; }
    public void changePhone(String phone) { this.phone = phone; }
    public void changeRole(String newRole) { this.role = newRole; }
    public void setHiredAt(LocalDate hiredAt) { this.hiredAt = hiredAt; }

    /**
     * 💡 [핵심 연쇄 저장 메서드] 
     * 인사팀이 'WORKER' 생성 시 호출하면 부모 객체 안에서 자식 객체를 생성해 양방향 링크를 걸어버립니다.
     * 이 메서드 덕분에 자바 단에서 save(user) 한 줄만 실행해도 2번 테이블까지 트랜잭션 내에서 한 세트로 연쇄 인서트가 끝납니다.
     */
    public void createRecoveryWorkerProfile(String empNumber, String department, String assignedDistrict, String certificate, String grade) {
        this.empNumber = empNumber;
        this.department = department;
        this.recoveryWorker = RecoveryWorker.builder()
                .assignedDistrict(assignedDistrict)
                .certificate(certificate)
                .grade(grade)
                .user(this) // 🔗 내 자신(부서/계정)을 자식 객체의 FK 자리에 매핑
                .build();
        
        // 사원으로 정식 임명되었으므로 입사 일자를 오늘 날짜로 자동 기록
        this.hiredAt = LocalDate.now();
    }
}
