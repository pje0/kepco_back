package com.kepco.complaint.dto;

public record AiAnalysisResult(
    Long workerId,
    int matchScore,
    String recommendationReason
) {}
