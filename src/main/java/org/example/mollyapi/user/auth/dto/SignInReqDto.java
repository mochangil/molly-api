package org.example.mollyapi.user.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;



public record SignInReqDto(
        @Schema(description = "이메일 형식에 맞춰주세요", example = "cats@cats.com")
        @Email(message = "이메일 형식이 아닙니다.")
        @NotBlank (message = "이메일은 필수값 입니다.")
        String email,
        @Schema(description = "비밀번호", example = "cats123456@")
        @NotBlank(message = "비밀번호는 필수값 입니다.")
        String password
) {
}
