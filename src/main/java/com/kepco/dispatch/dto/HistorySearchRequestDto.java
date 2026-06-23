package com.kepco.dispatch.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat; // 🚨 임포트 추가
import java.time.LocalDate;

@Data
public class HistorySearchRequestDto {
    
    // ⚡ [교정 완료] 하이픈 문자열 파싱 실패로 인한 시큐리티 403 오작동을 원천 차단합니다.
    @DateTimeFormat(pattern = "YYYY-MM-DD")
    private LocalDate startDate;

    @DateTimeFormat(pattern = "YYYY-MM-DD")
    private LocalDate endDate;

    private String region;       
    private String district;     
    private String aiCategory;   
    private String aiPriority;   
    private Long citizenId;      
}
