package com.kepco.dispatch.dto;

import lombok.Data;

@Data
public class AvailableWorkerDto {
    private Long id;            // DB: id -> !!recovery_worker 테이블의 PK ID!! (요청 보낼 때 중요)
    private String name;        // DB: users 테이블의 name -> 요원 실명
    private String grade;       // DB: recovery_worker.grade -> 숙련도 (JUNIOR, SENIOR 등)
    private String certificate; // DB: recovery_worker.certificate -> 자격증 목록
}
