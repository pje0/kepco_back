package com.kepco.notice.controller;

import com.kepco.notice.entity.NoticeAutoText;
import com.kepco.notice.repository.NoticeAutoTextRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notices/autotexts")
@RequiredArgsConstructor
public class NoticeAutoTextController {

    private final NoticeAutoTextRepository repository;

    @GetMapping
    public List<NoticeAutoText> getAll() {
        return repository.findAll();
    }

    @PostMapping
    public NoticeAutoText create(@RequestBody Map<String, String> body) {
        NoticeAutoText autoText = NoticeAutoText.builder()
                .shortcut(body.get("shortcut"))
                .replacement(body.get("replacement"))
                .build();
        return repository.save(autoText);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") Long id) {
        repository.deleteById(id);
    }
}