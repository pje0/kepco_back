package com.kepco.complaint.dto;

import com.kepco.complaint.entity.Complaint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ComplaintRequestDto(
    @NotNull(message = "시민 ID는 필수입니다.") 
    Long citizenId,
    
    @NotBlank(message = "민원 제목을 입력해주세요.") 
    String title,
    
    @NotBlank(message = "민원 내용을 상세히 입력해주세요.") 
    String content,
    
    String region,       // 시/도 (예: 부산광역시)
    String district,     // 시/군/구 (예: 해운대구)
    
    @NotBlank(message = "발생 장소 주소는 필수입니다.") 
    String address       // 상세 주소
) {
    // Service단에서 호출하기 위해 반드시 필요한 엔티티 변환 메서드
    public Complaint toEntity() {
        return Complaint.builder()
                .citizenId(citizenId)
                .title(title)
                .content(content)
                .region(region)
                .district(district)
                .address(address)
                .build();
    }
}
