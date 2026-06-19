package com.kepco.archive.controller;

import com.kepco.archive.service.ArchiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ArchiveController {

    private final ArchiveService archiveService;

    @GetMapping("/api/archive")
    public ResponseEntity<?> getArchives(@RequestParam(value = "category", required = false) String category) {
        try {
            return ResponseEntity.ok(archiveService.getArchives(category));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "자료실 목록 조회 실패: " + e.getMessage()));
        }
    }
}