package com.kepco.auth.dto;

import lombok.Data;

@Data
public class LoginRequestDto {
    private String username; // 로그인 시도 아이디
    private String password; // 로그인 시도 비밀번호
}
