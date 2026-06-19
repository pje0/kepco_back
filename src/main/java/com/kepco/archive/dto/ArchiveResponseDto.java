package com.kepco.archive.dto;

import com.kepco.archive.entity.Archive;
import com.kepco.archive.entity.ArchiveAttachment;
import lombok.Getter;

@Getter
public class ArchiveResponseDto {
    private final Long id;
    private final String title;
    private final String category;
    private final String fileType;
    private final String fileSize;
    private final String uploadedAt;
    private final Integer downloads;
    private final String fileUrl;

    public ArchiveResponseDto(Archive a) {
        this.id = a.getId();
        this.title = a.getTitle();
        this.category = a.getCategory();
        this.uploadedAt = a.getCreatedAt() != null ? a.getCreatedAt().toLocalDate().toString() : null;
        this.downloads = a.getDownloadCount();

        ArchiveAttachment att = (a.getAttachments() != null && !a.getAttachments().isEmpty())
                ? a.getAttachments().get(0) : null;
        if (att != null) {
            this.fileType = extractType(att.getFileName());
            this.fileSize = formatSize(att.getFileSize());
            this.fileUrl  = att.getFileUrl();
        } else {
            this.fileType = null;
            this.fileSize = null;
            this.fileUrl  = null;
        }
    }

    private static String extractType(String name) {
        if (name == null || !name.contains(".")) return null;
        return name.substring(name.lastIndexOf('.') + 1).toUpperCase();
    }

    private static String formatSize(Long bytes) {
        if (bytes == null) return null;
        if (bytes >= 1024 * 1024) return String.format("%.1fMB", bytes / (1024.0 * 1024.0));
        if (bytes >= 1024) return String.format("%.1fKB", bytes / 1024.0);
        return bytes + "B";
    }
}