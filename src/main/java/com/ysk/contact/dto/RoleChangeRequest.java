package com.ysk.contact.dto;

import com.ysk.contact.entity.UserRole;

import jakarta.validation.constraints.NotNull;

/**
 * 권한 변경 요청. 정의되지 않은 enum 값은 역직렬화 단계에서 400 으로 거부된다.
 */
public record RoleChangeRequest(
        @NotNull(message = "권한은 필수입니다.")
        UserRole role
) {}
