package com.kepco.dispatch.dto;

import lombok.Data;

@Data
public class PendingComplaintDto {
    private Long id;          // DB: id -> 민원 ID (선택 박스의 value)
    private String title;     // DB: title -> 민원 제목 (선택 박스의 텍스트 및 AI disasterType용)
    private String region;    // DB: region -> 시/도 (AI location용)
    private String district;  // DB: district -> 시/군/구 (AI location용)
    private String address;   // DB: address -> 상세 주소
}
