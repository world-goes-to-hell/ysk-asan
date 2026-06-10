package com.ysk.contact.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password,
        // "로그인 상태 유지" 체크 여부. JSON 에서 생략되면 primitive 기본값 false(기존 동작 유지).
        boolean rememberMe
) {}
