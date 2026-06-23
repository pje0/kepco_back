package com.kepco.complaint.controller;

import com.kepco.complaint.dto.ComplaintRequestDto;
import com.kepco.complaint.dto.RecommendedWorkerResponseDto;
import com.kepco.complaint.service.ComplaintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;

    @PostMapping("/complaints/register")
    public ResponseEntity<Void> registerComplaint(@RequestBody @Valid ComplaintRequestDto dto) {
        complaintService.registerComplaint(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/dispatch/complaints/{complaintId}/recommendations")
    public ResponseEntity<List<RecommendedWorkerResponseDto>> getAiRecommendedWorkers(
            @PathVariable("complaintId") Long complaintId) {
        
        List<RecommendedWorkerResponseDto> recommendations = complaintService.getRecommendedWorkers(complaintId);
        return ResponseEntity.ok(recommendations);
    }
}
