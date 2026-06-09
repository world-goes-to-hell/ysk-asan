package com.ysk.contact.dto;

import java.time.LocalDateTime;

import com.ysk.contact.entity.Contact;

public record ContactResponse(
        Long id,
        String department,
        String name,
        String email,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ContactResponse from(Contact contact) {
        return new ContactResponse(
                contact.getId(),
                contact.getDepartment(),
                contact.getName(),
                contact.getEmail(),
                contact.getCreatedAt(),
                contact.getUpdatedAt());
    }
}
