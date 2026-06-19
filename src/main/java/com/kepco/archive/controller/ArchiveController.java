package com.kepco.archive.controller;

import java.util.Map;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kepco.archive.entity.Archive;
import com.kepco.archive.entity.ArchiveAttachment;
import com.kepco.archive.repository.ArchiveRepository;
import com.kepco.archive.service.ArchiveService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ArchiveController {

    private final ArchiveService archiveService;
    private final ArchiveRepository archiveRepository;   // 추가: 주입

    @GetMapping("/api/archive")
    public ResponseEntity<?> getArchives(@RequestParam(value = "category", required = false) String category) {
        try {
            return ResponseEntity.ok(archiveService.getArchives(category));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "자료실 목록 조회 실패: " + e.getMessage()));
        }
    }

    @GetMapping("/api/archive/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) throws Exception {
        Archive archive = archiveRepository.findById(id)          // 클래스 → 주입받은 객체
                .orElseThrow(() -> new IllegalArgumentException("자료 없음"));
        ArchiveAttachment att = archive.getAttachments().stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("첨부파일 없음"));

        Resource resource = new FileSystemResource(att.getFileUrl());
        if (!resource.exists()) throw new IllegalArgumentException("파일이 존재하지 않습니다");

        String encoded = new String(att.getFileName().getBytes("UTF-8"), "ISO-8859-1");
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=" + encoded);
        return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    }
}