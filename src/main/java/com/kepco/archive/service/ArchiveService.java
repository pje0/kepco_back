package com.kepco.archive.service;

import com.kepco.archive.dto.ArchiveResponseDto;
import com.kepco.archive.repository.ArchiveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

import com.kepco.archive.entity.Archive;
import com.kepco.archive.entity.ArchiveAttachment;
import com.kepco.archive.repository.ArchiveAttachmentRepository;
import com.kepco.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ArchiveService {

    private final ArchiveRepository archiveRepository;
    
    private final ArchiveAttachmentRepository archiveAttachmentRepository;
    private final UserRepository userRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;
    
    @Transactional(readOnly = true)
    public List<ArchiveResponseDto> getArchives(String category) {
        List<com.kepco.archive.entity.Archive> archives =
                (category != null && !category.isBlank())
                        ? archiveRepository.findByCategoryWithAttachments(category)
                        : archiveRepository.findAllWithAttachments();
        return archives.stream().map(ArchiveResponseDto::new).collect(Collectors.toList());
    }
    
    @Transactional
    public void uploadArchive(String username, String title, String category,
                              String content, MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 없습니다.");
        }

        // 1. 작성자 확인 (로그인한 사용자)
        com.kepco.auth.entity.User writer = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계정입니다."));

        // 2. 저장 폴더 준비
        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

        // 3. UUID 붙여 실제 파일 저장
        String originalName = file.getOriginalFilename();
        String savedName = UUID.randomUUID() + "_" + originalName;
        File dest = new File(dir, savedName);
        file.transferTo(dest);

        // 4. archive 행 저장
        Archive archive = Archive.builder()
                .writerId(writer.getId())
                .department(writer.getDepartment())
                .category(category)
                .title(title)
                .content(content)
                .views(0)
                .downloadCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        archiveRepository.save(archive);

        // 5. archive_attachment 행 저장
        ArchiveAttachment att = ArchiveAttachment.builder()
                .archive(archive)
                .fileName(originalName)
                .fileUrl(dest.getAbsolutePath())
                .fileSize(file.getSize())
                .createdAt(LocalDateTime.now())
                .build();
        archiveAttachmentRepository.save(att);
    }
}