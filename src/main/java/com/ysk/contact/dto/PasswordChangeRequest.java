package com.ysk.contact.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 본인 비밀번호 변경 요청. 새 비밀번호 정책은 가입(RegisterRequest)과 동일하게 유지한다.
 */
public record PasswordChangeRequest(
        @NotBlank(message = "현재 비밀번호는 필수입니다.")
        String currentPassword,

        @NotBlank(message = "새 비밀번호는 필수입니다.")
        @Size(min = 8, max = 72, message = "새 비밀번호는 8~72자여야 합니다.")
        String newPassword
) {}
