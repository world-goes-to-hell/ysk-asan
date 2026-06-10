package com.ysk.contact.dto;

import java.time.LocalDateTime;

import com.ysk.contact.entity.User;

/**
 * 회원 관리(관리자 전용) 응답. 일반 {@link UserResponse} 와 달리 승인/잠금/실패 횟수까지 노출한다.
 */
public record AdminUserResponse(
        Long id,
        String username,
        String role,
        boolean approved,
        boolean locked,
        int failedLoginAttempts,
        LocalDateTime createdAt
) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole().name(),
                user.isApproved(),
                user.isLocked(),
                user.getFailedLoginAttempts(),
                user.getCreatedAt()
        );
    }
}
