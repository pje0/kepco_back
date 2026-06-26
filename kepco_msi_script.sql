-- ============================================================================
-- PostgreSQL 연결 아이콘 클릭 상태에서 F4 → Show all databases 체크 후 OK
-- 🚨 상단 툴바 public@postgres → public@MIS
-- STS, VSC 중단 → 전체 실행 → 커밋 → 테이블 새로고침
-- 전체 실행: alt + x
-- ============================================================================
-- MISCreate-v_log
-- 0.4 이메일 추가, roles 30 변경, login_id를 username으로 변경
-- 0.5 "user" 테이블명을 users로 변경, 소문자 변경
-- 0.6 notice 프로시저 추가
-- 0.7 데이터베이스 이름 mis 소문자 변경
-- 0.8 complaint에 ai_category, ai_priority 추가 / complaint_assignment 추가
-- 0.9 dispatch 프로시저 추가
-- 1.0 archive에 category, download_count 추가
-- 1.1 users에 emp_number, department 이전
-- 1.2 publish_at에 publish_at 추가
-- 1.3 notice_template, notice_boilerplate 테이블 추가
-- 1.4 notice_auto_text 테이블 추가
-- 1.5 dispatch 수정
-- 1.6 complaint 테이블의 빈 ai_category 컬럼에 ai_analysis 테이블의 카테고리 데이터를 동기화
-- ============================================================================

-- CREATE DATABASE "mis";

-- ============================================================================
-- 1. 기존 테이블 안전하게 모두 삭제 (CASCADE)
-- ============================================================================
DROP TABLE IF EXISTS complaint_assignment CASCADE;
DROP TABLE IF EXISTS dispatch CASCADE;
DROP TABLE IF EXISTS ai_recommended_worker CASCADE;
DROP TABLE IF EXISTS ai_analysis CASCADE;
DROP TABLE IF EXISTS complaint CASCADE;
DROP TABLE IF EXISTS notification CASCADE;
DROP TABLE IF EXISTS archive_attachment CASCADE;
DROP TABLE IF EXISTS archive CASCADE;
DROP TABLE IF EXISTS notice CASCADE;
DROP TABLE IF EXISTS notice_template CASCADE;
DROP TABLE IF EXISTS notice_boilerplate CASCADE;
DROP TABLE IF EXISTS notice_auto_text CASCADE;
DROP TABLE IF EXISTS recovery_worker CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- ============================================================================
-- 2. 핵심 테이블 생성
-- ============================================================================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE,
    phone VARCHAR(20),
    role VARCHAR(30) DEFAULT 'ROLE_CITIZEN', 
    hired_at DATE,
    emp_number VARCHAR(50) UNIQUE, 
    department VARCHAR(100),       
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE recovery_worker (
    id BIGSERIAL PRIMARY KEY,
    users_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    assigned_district VARCHAR(50) NOT NULL,
    certificate VARCHAR(255),
    grade VARCHAR(20),
    work_status VARCHAR(20) DEFAULT 'AVAILABLE',
    resigned_at TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE notice (
    id BIGSERIAL PRIMARY KEY,
    writer_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    department VARCHAR(100),
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    views INTEGER DEFAULT 0,
    is_pinned BOOLEAN DEFAULT FALSE,
    publish_at TIMESTAMP, 
    status VARCHAR(20) DEFAULT 'PUBLISHED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE notice_template (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    department VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE notice_boilerplate (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    department VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE notice_auto_text (
    id BIGSERIAL PRIMARY KEY,
    shortcut VARCHAR(50) NOT NULL UNIQUE,
    replacement VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE archive (
    id BIGSERIAL PRIMARY KEY,
    writer_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    department VARCHAR(100),
    category VARCHAR(50),                 
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    views INTEGER DEFAULT 0,
    download_count INTEGER DEFAULT 0,     
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE archive_attachment (
    id BIGSERIAL PRIMARY KEY,
    archive_id BIGINT NOT NULL REFERENCES archive(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    file_size BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE notification (
    id BIGSERIAL PRIMARY KEY,
    users_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    is_read BOOLEAN DEFAULT FALSE,
    reference_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE complaint (
    id BIGSERIAL PRIMARY KEY,
    citizen_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    region VARCHAR(50),
    district VARCHAR(50),
    address VARCHAR(300) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ai_category VARCHAR(50),
    ai_priority VARCHAR(20)
);

CREATE TABLE complaint_assignment (
    id BIGSERIAL PRIMARY KEY,
    complaint_id BIGINT NOT NULL REFERENCES complaint(id) ON DELETE CASCADE,
    worker_id BIGINT NOT NULL REFERENCES recovery_worker(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE TABLE ai_analysis (
    complaint_id BIGINT PRIMARY KEY REFERENCES complaint(id) ON DELETE CASCADE,
    category VARCHAR(50),
    confidence DECIMAL(5, 2),
    urgency VARCHAR(20) DEFAULT 'NORMAL',
    analyzed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_recommended_worker (
    id BIGSERIAL PRIMARY KEY,
    complaint_id BIGINT NOT NULL REFERENCES complaint(id) ON DELETE CASCADE,
    worker_id BIGINT NOT NULL REFERENCES recovery_worker(id) ON DELETE CASCADE,
    priority_rank INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE dispatch (
    id BIGSERIAL PRIMARY KEY,
    complaint_id BIGINT NOT NULL REFERENCES complaint(id) ON DELETE CASCADE,
    worker_id BIGINT NOT NULL REFERENCES recovery_worker(id) ON DELETE CASCADE,
    dispatcher_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) DEFAULT 'ASSIGNED',
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    arrived_at TIMESTAMP,
    completed_at TIMESTAMP,
    work_note TEXT
);

-- ============================================================================
-- 3. 기반 필수 계정 데이터 삽입 (복구팀 30명 명시적 대량 적재)
-- ============================================================================
INSERT INTO users (id, username, password, name, email, phone, role, hired_at, emp_number, department) VALUES 
(1, 'citizen1', '$2a$10$kHBcam9V2ObBNlnKUxtP6OawATwW8lhuFmWnqGrYYLylhMemCn4Wa', '김시민', 'citizen1@kepco.co.kr', '010-1111-0001', 'ROLE_CITIZEN', NULL, NULL, NULL), 
(2, 'citizen2', '$2a$10$kHBcam9V2ObBNlnKUxtP6OawATwW8lhuFmWnqGrYYLylhMemCn4Wa', '이시민', 'citizen2@kepco.co.kr', '010-1111-0002', 'ROLE_CITIZEN', NULL, NULL, NULL), 
(3, 'citizen3', '$2a$10$kHBcam9V2ObBNlnKUxtP6OawATwW8lhuFmWnqGrYYLylhMemCn4Wa', '박시민', 'citizen3@kepco.co.kr', '010-1111-0003', 'ROLE_CITIZEN', NULL, NULL, NULL), 
(4, 'citizen4', '$2a$10$kHBcam9V2ObBNlnKUxtP6OawATwW8lhuFmWnqGrYYLylhMemCn4Wa', '최시민', 'citizen4@kepco.co.kr', '010-1111-0004', 'ROLE_CITIZEN', NULL, NULL, NULL), 
(5, 'citizen5', '$2a$10$kHBcam9V2ObBNlnKUxtP6OawATwW8lhuFmWnqGrYYLylhMemCn4Wa', '정시민', 'citizen5@kepco.co.kr', '010-1111-0005', 'ROLE_CITIZEN', NULL, NULL, NULL), 
(6, 'admin', '$2a$10$kHBcam9V2ObBNlnKUxtP6OawATwW8lhuFmWnqGrYYLylhMemCn4Wa', '총괄관리자', 'admin@kepco.co.kr', '010-9999-9999', 'ROLE_ADMIN', '2015-01-01', 'EMP-2015001', '총괄관리부'), 
(7, 'hr_manager', '$2a$10$kHBcam9V2ObBNlnKUxtP6OawATwW8lhuFmWnqGrYYLylhMemCn4Wa', '인사담당자', 'hr@kepco.co.kr', '010-8888-8888', 'ROLE_HR', '2018-05-01', 'EMP-2018001', '인사관리팀'), 
(8, 'dispatcher1', '$2a$10$kHBcam9V2ObBNlnKUxtP6OawATwW8lhuFmWnqGrYYLylhMemCn4Wa', '파견반장', 'dispatch@kepco.co.kr', '010-7777-7777', 'ROLE_DISPATCHER', '2016-03-01', 'EMP-2016001', '시스템운영팀'), 
(9, 'worker1', '$2a$10$kHBcam9V2ObBNlnKUxtP6OawATwW8lhuFmWnqGrYYLylhMemCn4Wa', '강복구', 'worker1@kepco.co.kr', '010-2222-0001', 'ROLE_WORKER', '2019-01-01', 'EMP-W001', '부산지사 복구1팀'),
(10, 'worker2', '$2a$10$kHBcam9V2ObBNlnKUxtP6OawATwW8lhuFmWnqGrYYLylhMemCn4Wa', '윤복구', 'worker2@kepco.co.kr', '010-2222-0002', 'ROLE_WORKER', '2019-01-02', 'EMP-W002', '부산지사 복구2팀'),
(11, 'worker3', '$2a$10$kHBcam9V2ObBNlnKUxtP6OawATwW8lhuFmWnqGrYYLylhMemCn4Wa', '장복구', 'worker3@kepco.co.kr', '010-2222-0003', 'ROLE_WORKER', '2019-01-03', 'EMP-W003', '부산지사 복구3팀'),
(12, 'worker4', '$2a$10$kHBcam9V2ObBNlnKUxtP6OawATwW8lhuFmWnqGrYYLylhMemCn4Wa', '김복구', 'worker4@kepco.co.kr', '010-2222-0004', 'ROLE_WORKER', '2019-01-04', 'EMP-W004', '부산지사 복구4팀'),
(13, 'worker5', '$2a$10$kHBcam9V2ObBNlnKUxtP6OawATwW8lhuFmWnqGrYYLylhMemCn4Wa', '계복구', 'worker5@kepco.co.kr', '010-2222-0005', 'ROLE_WORKER', '2019-01-05', 'EMP-W005', '부산지사 복구1팀'),
(14, 'worker6', '$2a$10$kHBcam9V2ObBNlnKUxtP6OawATwW8lhuFmWnqGrYYLylhMemCn4Wa', '권복구', 'worker6@kepco.co.kr', '010-2222-0006', 'ROLE_WORKER', '2019-01-06', 'EMP-W006', '부산지사 복구2팀'),
(15, 'worker7', '$2a$10$kHBcam9V2ObBNlnKUxtP6OawATwW8lhuFmWnqGrYYLylhMemCn4Wa', '배복구', 'worker7@kepco.co.kr', '010-2222-0007', 'ROLE_WORKER', '2019-01-07', 'EMP-W007', '부산지사 복구3팀'),
(16, 'worker8', '$2a$10$kHBcam9V2ObBNlnKUxtP6OawATwW8lhuFmWnqGrYYLylhMemCn4Wa', '방복구', 'worker8@kepco.co.kr', '010-2222-0008', 'ROLE_WORKER', '2019-01-08', 'EMP-W008', '부산지사 복구4팀'),
(17, 'worker9', '$2a$10$kHBcam9V2ObBNlnKUxtP6OawATwW8lhuFmWnqGrYYLylhMemCn4Wa', '홍복구', 'worker9@kepco.co.kr', '010-2222-0009', 'ROLE_WORKER', '2019-01-09', 'EMP-W009', '부산지사 복구1팀'),
(18, 'worker10', '$2a$10$kHBcam9V2ObBNlnKUxtP6OawATwW8lhuFmWnqGrYYLylhMemCn4Wa', '박복구', 'worker10@kepco.co.kr', '010-2222-0010', 'ROLE_WORKER', '2019-01-10', 'EMP-W010', '부산지사 복구2팀'),
(19, 'worker11', '$2a$10$kHBcam9V2ObBNlnKUxtP6OawATwW8lhuFmWnqGrYYLylhMemCn4Wa', '조복구', 'worker11@kepco.co.kr', '010-2222-0011', 'ROLE_WORKER', '2019-01-11', 'EMP-W011', '부산지사 복구3팀'),
(20, 'worker12', '$2a$10$kHBcam9V2ObBNlnKUxtP6OawATwW8lhuFmWnqGrYYLylhMemCn4Wa', '정복구', 'worker12@kepco.co.kr', '010-2222-0012', 'ROLE_WORKER', '2019-01-12', 'EMP-W012', '부산지사 복구4팀'),
(21, 'worker13', '$2a$10$kHBcam9V2ObBNlnKUxtP6OawATwW8lhuFmWnqGrYYLylhMemCn4Wa', '현복구', 'worker13@kepco.co.kr', '010-2222-0013', 'ROLE_WORKER', '2019-01-13', 'EMP-W013', '부산지사 복구1팀'),
(22, 'worker14', '$2a$10$kHBcam9V2ObBNlnKUxtP6OawATwW8lhuFmWnqGrYYLylhMemCn4Wa', '남궁복구', 'worker14@kepco.co.kr', '010-2222-0014', 'ROLE_WORKER', '2019-01-14', 'EMP-W014', '부산지사 복구2팀'),
(23, 'worker15', '$2a$10$kHBcam9V2ObBNlnKUxtP6OawATwW8lhuFmWnqGrYYLylhMemCn4Wa', '제갈복구', 'worker15@kepco.co.kr', '010-2222-0015', 'ROLE_WORKER', '2019-01-15', 'EMP-W015', '부산지사 복구3팀'),
(24, 'worker16', '$2a$10$kHBcam9V2ObBNlnKUxtP6OawATwW8lhuFmWnqGrYYLylhMemCn4Wa', '청복구', 'worker16@kepco.co.kr', '010-2222-0016', 'ROLE_WORKER', '2019-01-16', 'EMP-W016', '부산지사 복구4팀'),
(25, 'worker17', '$2a$10$kHBcam9V2ObBNlnKUxtP6OawATwW8lhuFmWnqGrYYLylhMemCn4Wa', '준복구', 'worker17@kepco.co.kr', '010-2222-0017', 'ROLE_WORKER', '2019-01-17', 'EMP-W017', '부산지사 복구1팀'),
(26, 'worker18', '$2a$10$kHBcam9V2ObBNlnKUxtP6OawATwW8lhuFmWnqGrYYLylhMemCn4Wa', '모용복구', 'worker18@kepco.co.kr', '010-2222-0018', 'ROLE_WORKER', '2019-01-18', 'EMP-W018', '부산지사 복구2팀'),
(27, 'worker19', '$2a$10$kHBcam9V2ObBNlnKUxtP6OawATwW8lhuFmWnqGrYYLylhMemCn4Wa', '황보복구', 'worker19@kepco.co.kr', '010-2222-0019', 'ROLE_WORKER', '2019-01-19', 'EMP-W019', '부산지사 복구3팀'),
(28, 'worker20', '$2a$10$kHBcam9V2ObBNlnKUxtP6OawATwW8lhuFmWnqGrYYLylhMemCn4Wa', '태복구', 'worker20@kepco.co.kr', '010-2222-0020', 'ROLE_WORKER', '2019-01-20', 'EMP-W020', '부산지사 복구4팀'),
(29, 'worker21', '$2a$10$kHBcam9V2ObBNlnKUxtP6OawATwW8lhuFmWnqGrYYLylhMemCn4Wa', '기복구', 'worker21@kepco.co.kr', '010-2222-0021', 'ROLE_WORKER', '2019-01-21', 'EMP-W021', '부산지사 복구1팀'),
(30, 'worker22', '$2a$10$kHBcam9V2ObBNlnKUxtP6OawATwW8lhuFmWnqGrYYLylhMemCn4Wa', '남복구', 'worker22@kepco.co.kr', '010-2222-0022', 'ROLE_WORKER', '2019-01-22', 'EMP-W022', '부산지사 복구2팀'),
(31, 'worker23', '$2a$10$kHBcam9V2ObBNlnKUxtP6OawATwW8lhuFmWnqGrYYLylhMemCn4Wa', '유복구', 'worker23@kepco.co.kr', '010-2222-0023', 'ROLE_WORKER', '2019-01-23', 'EMP-W023', '부산지사 복구3팀'),
(32, 'worker24', '$2a$10$kHBcam9V2ObBNlnKUxtP6OawATwW8lhuFmWnqGrYYLylhMemCn4Wa', '이복구', 'worker24@kepco.co.kr', '010-2222-0024', 'ROLE_WORKER', '2019-01-24', 'EMP-W024', '부산지사 복구4팀'),
(33, 'worker25', '$2a$10$kHBcam9V2ObBNlnKUxtP6OawATwW8lhuFmWnqGrYYLylhMemCn4Wa', '양복구', 'worker25@kepco.co.kr', '010-2222-0025', 'ROLE_WORKER', '2019-01-25', 'EMP-W025', '부산지사 복구1팀'),
(34, 'worker26', '$2a$10$kHBcam9V2ObBNlnKUxtP6OawATwW8lhuFmWnqGrYYLylhMemCn4Wa', '진복구', 'worker26@kepco.co.kr', '010-2222-0026', 'ROLE_WORKER', '2019-01-26', 'EMP-W026', '부산지사 복구2팀'),
(35, 'worker27', '$2a$10$kHBcam9V2ObBNlnKUxtP6OawATwW8lhuFmWnqGrYYLylhMemCn4Wa', '차복구', 'worker27@kepco.co.kr', '010-2222-0027', 'ROLE_WORKER', '2019-01-27', 'EMP-W027', '부산지사 복구3팀'),
(36, 'worker28', '$2a$10$kHBcam9V2ObBNlnKUxtP6OawATwW8lhuFmWnqGrYYLylhMemCn4Wa', '원복구', 'worker28@kepco.co.kr', '010-2222-0028', 'ROLE_WORKER', '2019-01-28', 'EMP-W028', '부산지사 복구4팀'),
(37, 'worker29', '$2a$10$kHBcam9V2ObBNlnKUxtP6OawATwW8lhuFmWnqGrYYLylhMemCn4Wa', '제복구', 'worker29@kepco.co.kr', '010-2222-0029', 'ROLE_WORKER', '2019-01-29', 'EMP-W029', '부산지사 복구1팀'),
(38, 'worker30', '$2a$10$kHBcam9V2ObBNlnKUxtP6OawATwW8lhuFmWnqGrYYLylhMemCn4Wa', '왕복구', 'worker30@kepco.co.kr', '010-2222-0030', 'ROLE_WORKER', '2019-01-30', 'EMP-W030', '부산지사 복구2팀');

INSERT INTO recovery_worker (users_id, assigned_district, certificate, grade, work_status) VALUES 
(9, '해운대구', '전기공사기사, 소방설비기사', 'MASTER', 'AVAILABLE'), 
(10, '수영구', '전기산업기사', 'SENIOR', 'DISPATCHED'), 
(11, '남구', '없음', 'JUNIOR', 'AVAILABLE'),
(12, '동래구', '전기공사기사', 'MASTER', 'DISPATCHED'),
(13, '해운대구', '전기산업기사', 'SENIOR', 'AVAILABLE'),
(14, '수영구', '없음', 'JUNIOR', 'AVAILABLE'),
(15, '남구', '전기공사기사', 'MASTER', 'AVAILABLE'),
(16, '동래구', '소방설비기사', 'SENIOR', 'DISPATCHED'),
(17, '해운대구', '없음', 'JUNIOR', 'AVAILABLE'),
(18, '수영구', '전기산업기사', 'MASTER', 'AVAILABLE'),
(19, '남구', '전기공사기사', 'SENIOR', 'AVAILABLE'),
(20, '동래구', '없음', 'JUNIOR', 'DISPATCHED'),
(21, '해운대구', '전기공사기사', 'MASTER', 'AVAILABLE'),
(22, '수영구', '전기산업기사', 'SENIOR', 'AVAILABLE'),
(23, '남구', '소방설비기사', 'JUNIOR', 'AVAILABLE'),
(24, '동래구', '전기공사기사', 'MASTER', 'DISPATCHED'),
(25, '해운대구', '없음', 'SENIOR', 'AVAILABLE'),
(26, '수영구', '전기산업기사', 'JUNIOR', 'AVAILABLE'),
(27, '남구', '전기공사기사', 'MASTER', 'AVAILABLE'),
(28, '동래구', '소방설비기사', 'SENIOR', 'DISPATCHED'),
(29, '해운대구', '전기산업기사', 'JUNIOR', 'AVAILABLE'),
(30, '수영구', '전기공사기사', 'MASTER', 'AVAILABLE'),
(31, '남구', '없음', 'SENIOR', 'AVAILABLE'),
(32, '동래구', '전기산업기사', 'JUNIOR', 'DISPATCHED'),
(33, '해운대구', '전기공사기사', 'MASTER', 'AVAILABLE'),
(34, '수영구', '소방설비기사', 'SENIOR', 'AVAILABLE'),
(35, '남구', '전기산업기사', 'JUNIOR', 'AVAILABLE'),
(36, '동래구', '없음', 'MASTER', 'DISPATCHED'),
(37, '해운대구', '전기공사기사', 'SENIOR', 'AVAILABLE'),
(38, '수영구', '전기산업기사', 'JUNIOR', 'AVAILABLE');

-- 🚨 시퀀스 사전 동기화 (이후 프로시저 자동 증가를 위함)
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users)); 
SELECT setval('recovery_worker_id_seq', (SELECT MAX(id) FROM recovery_worker));

-- ============================================================================
-- 4. [신규] 복구팀 인원 대량 자동 생성 프로시저
-- ============================================================================
CREATE OR REPLACE PROCEDURE generate_worker_dummy_data()
LANGUAGE plpgsql
AS $BODY$
DECLARE
    i INT;
    v_user_id BIGINT;
    v_district VARCHAR(50);
    v_grade VARCHAR(20);
    v_status VARCHAR(20);
    v_random_val INT;
    v_last_names VARCHAR := '김이박최정강조윤장임한오서신권황안송전홍유고문양손배백허남심노하곽성차주우구';
    v_name VARCHAR(100);
BEGIN
    FOR i IN 31..130 LOOP
        v_random_val := floor(random() * 4);
        v_district := CASE v_random_val WHEN 0 THEN '해운대구' WHEN 1 THEN '수영구' WHEN 2 THEN '남구' ELSE '동래구' END;

        v_random_val := floor(random() * 3);
        v_grade := CASE v_random_val WHEN 0 THEN 'JUNIOR' WHEN 1 THEN 'SENIOR' ELSE 'MASTER' END;

        v_random_val := floor(random() * 5);
        v_status := CASE WHEN v_random_val < 3 THEN 'AVAILABLE' ELSE 'DISPATCHED' END;

        -- 한국의 대표적인 성씨 목록에서 무작위로 하나를 추출한 뒤 '복구'를 붙여 이름 생성
        v_name := substr(v_last_names, floor(random() * length(v_last_names) + 1)::int, 1) || '복구';

        INSERT INTO users (username, password, name, email, phone, role, hired_at, emp_number, department)
        VALUES ('worker' || i, '$2a$10$kHBcam9V2ObBNlnKUxtP6OawATwW8lhuFmWnqGrYYLylhMemCn4Wa', v_name, 'worker' || i || '@kepco.co.kr', '010-4444-' || lpad(i::text, 4, '0'), 'ROLE_WORKER', NOW() - (random() * interval '1500 days'), 'EMP-202' || floor(random() * 5 + 1) || lpad(i::text, 3, '0'), '부산지사 복구' || floor(random() * 4 + 1) || '팀')
        RETURNING id INTO v_user_id;

        INSERT INTO recovery_worker (users_id, assigned_district, certificate, grade, work_status)
        VALUES (v_user_id, v_district, CASE WHEN random() > 0.5 THEN '전기공사기사' ELSE '없음' END, v_grade, v_status);
    END LOOP;
END;
$BODY$;

-- ============================================================================
-- 5. 공지사항 생성 프로시저
-- ============================================================================
CREATE OR REPLACE PROCEDURE generate_notice_dummy_data()
LANGUAGE plpgsql
AS $BODY$
DECLARE
    i INT;
    v_writer_id BIGINT;
    v_department VARCHAR(100);
    v_title VARCHAR(255);
    v_is_pinned BOOLEAN;
    v_views INTEGER;
    v_random_dept INT;
BEGIN
    FOR i IN 1..150 LOOP
        v_writer_id := CASE WHEN random() > 0.5 THEN 6 ELSE 7 END;
        v_random_dept := floor(random() * 4);
        v_department := CASE v_random_dept WHEN 0 THEN '총괄관리부' WHEN 1 THEN '인사관리팀' WHEN 2 THEN '시스템운영팀' ELSE '안전관리본부' END;
        v_is_pinned := random() < 0.1;
        v_views := floor(random() * 500);
        IF v_is_pinned THEN v_title := '[필독] 시스템 중요 공지 및 안내 사항'; ELSE v_title := '일반 공지사항 등록 테스트입니다'; END IF;
        
        INSERT INTO notice (writer_id, department, title, content, views, is_pinned, publish_at, status, created_at, updated_at)
        VALUES (v_writer_id, v_department, v_title, '공지사항 본문 테스트 데이터입니다. 페이징 및 정렬이 정상적으로 이루어지는지 확인하기 위한 더미 내용입니다.', v_views, v_is_pinned, NULL, 'PUBLISHED', NOW() - (random() * interval '100 days'), NOW() - (random() * interval '10 days'));
    END LOOP;
END;
$BODY$;

-- ============================================================================
-- 6. 자료실 및 첨부파일 생성 프로시저 
-- ============================================================================
CREATE OR REPLACE PROCEDURE generate_archive_dummy_data()
LANGUAGE plpgsql
AS $BODY$
DECLARE
    i INT;
    v_writer_id BIGINT;
    v_archive_id BIGINT;
    v_category VARCHAR(50);
    v_random_category INT;
    v_download_count INTEGER;
BEGIN
    FOR i IN 1..60 LOOP
        v_writer_id := CASE WHEN random() > 0.5 THEN 6 ELSE 7 END;
        v_random_category := floor(random() * 4);
        v_category := CASE v_random_category WHEN 0 THEN '매뉴얼' WHEN 1 THEN '교육자료' WHEN 2 THEN '보고서' ELSE '서식' END;
        v_download_count := floor(random() * 150);

        INSERT INTO archive (writer_id, department, category, title, content, views, download_count, created_at, updated_at)
        VALUES (v_writer_id, '안전관리본부', v_category, '현장 복구 ' || v_category || ' (개정판 v' || i || '.0)', '현장 복구 시 지켜야 할 필수 ' || v_category || '입니다. 반드시 다운로드하여 숙지하시기 바랍니다. 자료 번호: ' || i, floor(random() * 300), v_download_count, NOW() - (random() * interval '60 days'), NOW() - (random() * interval '5 days')) 
        RETURNING id INTO v_archive_id;

        IF random() > 0.3 THEN
            INSERT INTO archive_attachment (archive_id, file_name, file_url, file_size)
            VALUES (v_archive_id, '한전_안전관리_' || v_category || '_' || i || '.pdf', 'https://kepco.co.kr/assets/guide_' || i || '.pdf', floor(random() * 5000000 + 1000000));
        END IF;
    END LOOP;
END;
$BODY$;

-- ============================================================================
-- 7. 비즈니스 대량 데이터(120건) 생성 프로시저 (대시보드 통계 대응 완벽화)
-- ============================================================================
CREATE OR REPLACE PROCEDURE generate_large_dummy_data()
LANGUAGE plpgsql
AS $BODY$
DECLARE
    i INT;
    v_complaint_id BIGINT;
    v_citizen_id BIGINT;
    v_urgency VARCHAR(20);      
    v_status VARCHAR(20);       
    v_region VARCHAR(50) := '부산광역시';
    v_district VARCHAR(50);     
    v_category VARCHAR(50);     
    v_confidence DECIMAL(5,2);
    v_worker_pk_1 BIGINT;
    v_worker_pk_2 BIGINT;
    v_random_val INT;
BEGIN
    FOR i IN 1..120 LOOP
        v_citizen_id := floor(random() * 5 + 1); 
        v_random_val := floor(random() * 4);
        v_district := CASE v_random_val WHEN 0 THEN '해운대구' WHEN 1 THEN '수영구' WHEN 2 THEN '남구' ELSE '동래구' END;
        
        -- 🚨 120건을 40건씩 3등분하여 PENDING, IN_PROGRESS, RESOLVED 상태를 정확하게 고정 배분
        v_status := CASE 
            WHEN i <= 40 THEN 'PENDING' 
            WHEN i <= 80 THEN 'IN_PROGRESS' 
            ELSE 'RESOLVED' 
        END;
        
        -- 🚨 카테고리 및 우선순위 변수를 삽입 전에 미리 생성
        v_random_val := floor(random() * 4);
        v_category := CASE v_random_val WHEN 0 THEN '단전' WHEN 1 THEN '전선끊어짐' WHEN 2 THEN '스파크' ELSE '변압기이상' END;
        v_confidence := random() * 15 + 80; 
        v_random_val := floor(random() * 4);
        v_urgency := CASE v_random_val WHEN 0 THEN 'LOW' WHEN 1 THEN 'NORMAL' WHEN 2 THEN 'HIGH' ELSE 'EMERGENCY' END;
        
        -- 🚨 complaint 테이블 INSERT 시 카테고리(ai_category) 데이터를 즉시 주입하여 대시보드 통계 누락 방지
        INSERT INTO complaint (citizen_id, title, content, region, district, address, status, created_at, ai_category, ai_priority)
        VALUES (v_citizen_id, '전력 설비 이상 신고 건 - ' || i, '현장에 문제가 발생했습니다. 신속한 확인 및 조치 부탁드립니다.', v_region, v_district, v_region || ' ' || v_district || ' ' || (floor(random() * 100 + 1)) || '번길', v_status, NOW() - (random() * interval '30 days'), v_category, v_urgency) RETURNING id INTO v_complaint_id;
        
        INSERT INTO ai_analysis (complaint_id, category, confidence, urgency, analyzed_at)
        VALUES (v_complaint_id, v_category, v_confidence, v_urgency, NOW() - (random() * interval '29 days'));
        
        -- 🚨 명시적으로 삽입된 30명의 복구 대원(PK 9~38) 내에서만 배정되도록 범위 축소 및 안전성 확보
        v_worker_pk_1 := floor(random() * 30) + 9; 
        v_worker_pk_2 := CASE WHEN v_worker_pk_1 = 38 THEN 9 ELSE v_worker_pk_1 + 1 END;
        
        INSERT INTO ai_recommended_worker (complaint_id, worker_id, priority_rank) VALUES (v_complaint_id, v_worker_pk_1, 1), (v_complaint_id, v_worker_pk_2, 2);
        
        IF v_status != 'PENDING' THEN
            INSERT INTO dispatch (complaint_id, worker_id, dispatcher_id, status, assigned_at, arrived_at, completed_at, work_note)
            VALUES (v_complaint_id, v_worker_pk_1, 8, v_status, NOW() - interval '2 days', NOW() - interval '1 day', CASE WHEN v_status = 'RESOLVED' THEN NOW() ELSE NULL END, CASE WHEN v_status = 'RESOLVED' THEN '현장 출동 및 정비 작업 조치 완료.' ELSE NULL END);
            
            INSERT INTO notification (users_id, type, title, content, reference_id)
            VALUES (v_citizen_id, 'STATUS_UPDATED', '민원 처리 상태 변경 안내', '신고하신 전력 민원이 현재 [' || v_status || '] 상태로 변경 처리되었습니다.', v_complaint_id);
        END IF;
    END LOOP;
END;
$BODY$;

-- ============================================================================
-- 8. 템플릿 및 상용구 초기 더미 데이터 삽입
-- ============================================================================
INSERT INTO notice_template (title, content, department) VALUES 
('정기 시스템 점검 안내', '<p>안녕하십니까, 시스템운영팀입니다.</p><p>안정적인 서비스 제공을 위해 아래와 같이 정기 시스템 점검을 진행합니다.</p><p><br></p><p><strong>1. 점검 일시:</strong> 2026년 O월 O일 OO:OO ~ OO:OO</p><p><strong>2. 대상 시스템:</strong> MIS 전체</p><p><strong>3. 작업 내용:</strong> 서버 안정화 및 패치</p><p><br></p><p>작업 시간 동안 시스템 접속이 단절될 수 있으니 양해 부탁드립니다.</p>', '시스템운영팀'),
('신규 기능 배포 안내', '<p>본 시스템의 신규 기능이 업데이트되었습니다. 상세 내역은 아래를 참고해 주시기 바랍니다.</p>', '총괄관리부');

INSERT INTO notice_boilerplate (title, content, department) VALUES 
('기본 인사말 (시스템운영팀)', '<p>안녕하십니까, 시스템운영팀입니다.</p>', '시스템운영팀'),
('문의처 하단 꼬리말', '<p><br></p><hr><p><strong>[문의처]</strong><br>내선번호: 02-1234-5678<br>이메일: mis_support@kepco.co.kr</p>', '전체'),
('장애 발생 시 행동 요령', '<p><strong>※ 장애 발생 시 행동 요령:</strong> 즉시 파견반장에게 무전으로 보고하고, 모바일 앱에서 "긴급 장애 발생" 버튼을 터치하여 현장 상황을 동기화해주십시오.</p>', '안전관리본부'),
('개인정보 취급 주의 안내', '<p><strong>※ 개인정보 보호법 제15조</strong>에 의거하여, 본 게시물에 포함된 시민의 개인정보(연락처, 주소 등)를 무단으로 유출하거나 목적 외로 사용할 경우 법적 처벌을 받을 수 있으니 각별히 주의하시기 바랍니다.</p>', '전체'),
('보안 서약서 양식 하단', '<p><br></p><p>위 본인은 상기 보안 수칙을 숙지하였으며, 이를 위반할 시 사규에 따른 징계 조치를 감수할 것을 서약합니다.</p>', '인사관리팀');

INSERT INTO notice_auto_text (shortcut, replacement) VALUES 
('있따', '있다'), 
('안되', '안 돼'), 
('되서', '돼서'),
('몇일', '며칠'),
('바램', '바람'),
('어의없다', '어이없다'),
('금새', '금세'),
('요컨데', '요컨대'),
('결제바랍니다', '결재 바랍니다'),
('ㅎㅈ', '한국전력공사'),
('ㅈㅅ', '죄송합니다.'),
('ㄱㅅ', '감사합니다.'),
('ㅅㄱ', '수고하셨습니다.'),
('/점검', '[안내] 정기 시스템 점검 및 네트워크 단전 안내'),
('/인사', '안녕하십니까, 한국전력공사 안전관리본부입니다.'),
('/문의', '본 공지와 관련한 문의사항은 시스템운영팀(내선 1122)으로 연락해 주시기 바랍니다.'),
('/승인', '해당 건에 대해 정상적으로 승인 처리 완료하였습니다.'),
('/반려', '기재된 내용이 불충분하여 반려합니다. 사유를 확인하시고 재작성 후 상신 바랍니다.'),
('/완료', '현장 복구 및 후속 안전 조치가 모두 완료되었습니다.'),
('/긴급', '[긴급] 전력 설비 장애 발생으로 인한 긴급 복구 작업 안내');


-- ============================================================================
-- 9. 프로시저 호출 및 특수 데이터 조작
-- ============================================================================
BEGIN;

CALL generate_worker_dummy_data();
CALL generate_notice_dummy_data();
CALL generate_archive_dummy_data();
CALL generate_large_dummy_data();

UPDATE complaint SET status = 'RESOLVED' WHERE id = 1;

UPDATE dispatch
SET status = 'RESOLVED',
    completed_at = '2026-06-20 14:00:00'
WHERE complaint_id = 1;

UPDATE dispatch
SET status = 'RESOLVED',
    completed_at = '2026-06-20 14:00:00'
WHERE id = (SELECT id FROM dispatch LIMIT 1);

COMMIT;


-- ============================================================================
-- 10. 배정 이력 백업 테이블 동기화 및 인덱스/Sequence 정리
-- ============================================================================
INSERT INTO complaint_assignment (complaint_id, worker_id, assigned_at, completed_at)
SELECT complaint_id, worker_id, assigned_at, completed_at FROM dispatch;

DROP INDEX IF EXISTS idx_dispatch_resolved_completed_at;
DROP INDEX IF EXISTS idx_dispatch_complaint_id;
DROP INDEX IF EXISTS idx_dispatch_history_resolved_scan;
DROP INDEX IF EXISTS idx_dispatch_complaint_fk_idx;

CREATE INDEX idx_dispatch_history_resolved_scan
ON dispatch (completed_at)
WHERE status = 'RESOLVED';

CREATE INDEX idx_dispatch_complaint_fk_idx
ON dispatch (complaint_id);

SELECT setval('users_id_seq', (SELECT MAX(id) FROM users)); 
SELECT setval('recovery_worker_id_seq', (SELECT MAX(id) FROM recovery_worker));
SELECT setval('notice_template_id_seq', (SELECT MAX(id) FROM notice_template)); 
SELECT setval('notice_boilerplate_id_seq', (SELECT MAX(id) FROM notice_boilerplate));
SELECT setval('notice_auto_text_id_seq', (SELECT MAX(id) FROM notice_auto_text));

-- ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
-- 📊 [그랜드 마감 검증]
-- ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
SELECT 'complaint' AS table_name, status, COUNT(*) FROM complaint GROUP BY status
UNION ALL
SELECT 'dispatch' AS table_name, status, COUNT(*) FROM dispatch GROUP BY status
UNION ALL
SELECT 'recovery_worker' AS table_name, work_status, COUNT(*) FROM recovery_worker GROUP BY work_status;