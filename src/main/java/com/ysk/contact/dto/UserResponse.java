package com.ysk.contact.dto;

import java.time.LocalDateTime;

import com.ysk.contact.entity.User;

public record UserResponse(Long id, String username, String role, LocalDateTime createdAt) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRole().name(), user.getCreatedAt());
    }
}
