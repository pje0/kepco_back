package com.kepco.auth.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UserUpdateRequestDto {

    private String password; // ⭕ DB: password (변경할 새 비밀번호 - 선택)
    
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;    // ⭕ DB: email (변경할 새 이메일 주소 - 선택)
    
    private String phone;    // ⭕ DB: phone (변경할 새 연락처 번호 - 선택)
}
