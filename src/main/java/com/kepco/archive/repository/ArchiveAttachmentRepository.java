package com.kepco.archive.repository;

import com.kepco.archive.entity.ArchiveAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArchiveAttachmentRepository extends JpaRepository<ArchiveAttachment, Long> {
}