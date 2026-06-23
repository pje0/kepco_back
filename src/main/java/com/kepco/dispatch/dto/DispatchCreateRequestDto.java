package com.kepco.dispatch.dto;

import lombok.Data;

@Data
public class DispatchCreateRequestDto {
    private Long complaintId; // DB: complaint_id -> 민원 ID
    private Long workerId;    // DB: worker_id -> recovery_worker 테이블의 PK ID!! (주의: user_id 아님)
    private String workNote;  // DB: work_note -> 지시 사항
}
