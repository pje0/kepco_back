package com.kepco.auth.controller;

import com.kepco.auth.dto.RegisterRequestDto;
import com.kepco.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/auth") // ⭕ 기존 "/api/auth"에서 "/auth"로 변경!
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register") // ➡️ 이제 이 API 주소는 "/auth/register"가 됩니다.
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequestDto registerRequest) {
        try {
            authService.register(registerRequest);
            return ResponseEntity.ok(Map.of("message", "한전 MIS 시스템에 회원가입이 완료되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
