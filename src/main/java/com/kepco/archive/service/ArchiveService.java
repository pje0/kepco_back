package com.kepco.archive.service;

import com.kepco.archive.dto.ArchiveResponseDto;
import com.kepco.archive.repository.ArchiveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArchiveService {

    private final ArchiveRepository archiveRepository;

    @Transactional(readOnly = true)
    public List<ArchiveResponseDto> getArchives(String category) {
        List<com.kepco.archive.entity.Archive> archives =
                (category != null && !category.isBlank())
                        ? archiveRepository.findByCategoryWithAttachments(category)
                        : archiveRepository.findAllWithAttachments();
        return archives.stream().map(ArchiveResponseDto::new).collect(Collectors.toList());
    }
}