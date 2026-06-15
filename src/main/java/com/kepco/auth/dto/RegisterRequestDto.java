package com.kepco.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data	
public class RegisterRequestDto {

    @NotBlank(message = "아이디는 필수 입력 항목입니다.")
    @Size(min = 4, max = 50, message = "아이디는 4자 이상 50자 이하로 입력해 주세요.")
    private String username;

    @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
    @Size(min = 6, max = 100, message = "비밀번호는 6자 이상 입력해 주세요.")
    private String password;

    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;
}
