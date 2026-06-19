package com.kepco.archive.repository;

import com.kepco.archive.entity.Archive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ArchiveRepository extends JpaRepository<Archive, Long> {

    @Query("SELECT DISTINCT a FROM Archive a LEFT JOIN FETCH a.attachments ORDER BY a.createdAt DESC")
    List<Archive> findAllWithAttachments();

    @Query("SELECT DISTINCT a FROM Archive a LEFT JOIN FETCH a.attachments WHERE a.category = :category ORDER BY a.createdAt DESC")
    List<Archive> findByCategoryWithAttachments(@Param("category") String category);
}