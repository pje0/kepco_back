
<div align="center">

# ⚡ 한전(KEPCO) 정전·장애 대응 MIS — Backend

시민 정전·장애 신고 접수, AI 기반 작업자 추천, 출동 처리를 지원하는 정전·장애 대응 관리 시스템

[![Java](https://img.shields.io/badge/Java-17-007396?logo=openjdk&logoColor=white)](.)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3-6DB33F?logo=springboot&logoColor=white)](.)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?logo=postgresql&logoColor=white)](.)
[![JWT](https://img.shields.io/badge/Auth-JWT-black?logo=jsonwebtokens&logoColor=white)](.)

프론트엔드 레포 → [kepco-front](https://github.com/pje0/kepco_front)

</div>

---

## 📋 목차

- [소개](#-소개)
- [주요 기능](#-주요-기능)
- [기술 스택](#-기술-스택)
- [아키텍처](#-아키텍처)
- [ERD](#-erd)
- [시작하기](#-시작하기)
- [API 명세](#-api-명세)
- [폴더 구조](#-폴더-구조)
- [팀원](#-팀원)

---

## 📖 소개

- 시민 정전·장애 신고 접수 (카카오 우편번호 API 연동)
- AI 기반 신고 분류 및 작업자 추천
- JWT 기반 인증 및 역할별 접근 제어
- 출동 담당자 소유권 검증을 통한 데이터 무결성 확보

---

## ✨ 주요 기능

| 기능 | 설명 |
|---|---|
| 신고 접수 | 시민 정전·장애 신고, 주소 검색(카카오 우편번호) |
| AI 작업자 추천 | 신고 내용 기반 자동 분류 및 추천 |
| 출동 관리 | 배정된 출동 건 상태 관리, 소유권 검증 |
| 자료실 | 인증 기반 파일 업로드/다운로드 |
| 인사관리 | 직원 계정 및 권한 관리 |

---

## 🛠 기술 스택

**Language** · Java 17

**Framework** · Spring Boot 3, Spring Security

**Database** · PostgreSQL (port 5433, DB: `mis`)

**인증** · JWT

**외부 API** · 카카오 우편번호 API

**Tool** · STS4, DBeaver, Git, Notion

---

## 🏗 아키텍처

### 패키지 구조 (도메인 기반)

```
com.kepco
├── auth        # JWT 발급/검증, 로그인
├── report      # 신고 접수
├── dispatch    # AI 분류, 작업자 추천
├── work        # 출동확인
├── archive     # 자료실
├── user        # 인사관리
└── common      # 공통 응답 포맷, 예외 처리
```

### 인증 흐름
```
로그인 → JWT 발급 → Authorization 헤더로 매 요청 전달
   → JwtAuthenticationFilter 검증 → 역할 기반 접근 제어
```

---

## 🗃 ERD

주요 테이블: `users`(emp_number, department), `report`, `dispatch`, `archive`, `archive_attachment`, `work_log`

> 전체 ERD 이미지: `/docs/erd.png`

---

## 🚀 시작하기

### 요구 사항
- JDK 17
- PostgreSQL
- 카카오 우편번호 API 키

### 설치 및 실행

```bash
git clone https://github.com/{your-org}/kepco-mis-backend.git
cd kepco-mis-backend
```

```sql
CREATE DATABASE mis;
```

`src/main/resources/application.yml`에 DB, JWT, API 키 정보 입력 후:

```bash
./gradlew bootRun
```

```
http://localhost:8080
```

---

## 📡 API 명세

| Method | Endpoint | 설명 |
|---|---|---|
| POST | `/api/auth/login` | 로그인, JWT 발급 |
| POST | `/api/reports` | 신고 접수 |
| GET | `/api/dispatch/recommend/{reportId}` | AI 작업자 추천 |
| GET | `/api/works/my` | 본인 배정 출동 건 조회 |
| PATCH | `/api/works/{workId}/status` | 출동 상태 변경 |
| GET | `/api/archives` | 자료실 목록 조회 |
| POST | `/api/archives` | 자료실 파일 업로드 |
| GET | `/api/archives/{id}/download` | 파일 다운로드 |

> 상세 API 명세: `/docs/api-spec.md`

---

## 📁 폴더 구조

```
kepco-mis-backend
├── src/main/java/com/kepco
│   ├── auth
│   ├── report
│   ├── dispatch
│   ├── work
│   ├── archive
│   ├── user
│   └── common
├── src/main/resources
│   └── application.yml
└── build.gradle
```

---

## 👥 팀원

| 이름 | GitHub |
|---|---|
| 박정은 | [@github-id](https://github.com/) |
| 조성민 | [@github-id](https://github.com/) |
| 홍현민 | [@github-id](https://github.com/) |
