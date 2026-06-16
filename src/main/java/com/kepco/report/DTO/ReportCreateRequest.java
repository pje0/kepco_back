package com.kepco.report.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportCreateRequest {
    private Long citizenId;
    private String citizenName;
    private String citizenPhone;
    private String title;
    private String content;
    private String address;
    private String roadAddress;
    private String district;
    private String category; // DB complaint 테이블에는 없고, 이후 AI 분석 테이블에 들어갈 데이터
    private String severity; 
}