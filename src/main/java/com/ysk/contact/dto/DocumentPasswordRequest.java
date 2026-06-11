package com.ysk.contact.dto;

import jakarta.validation.constraints.NotBlank;

/** 공개 열람/직인 엔드포인트의 비밀번호 확인 요청. */
public record DocumentPasswordRequest(
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {}
