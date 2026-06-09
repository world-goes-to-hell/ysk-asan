package com.ysk.contact.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "사용자명은 필수입니다.")
        @Size(min = 3, max = 30, message = "사용자명은 3~30자여야 합니다.")
        String username,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 72, message = "비밀번호는 8~72자여야 합니다.")
        String password
) {}
