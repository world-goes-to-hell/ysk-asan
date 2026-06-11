package com.ysk.contact.dto;

import java.time.LocalDateTime;

import com.ysk.contact.entity.OfficialDocument;

/**
 * 발급자 본인의 공문 목록 항목. sealed 는 sealContentType 존재로 판정해
 * LAZY BLOB(sealImage) 로딩 없이 직인 여부를 알려준다.
 */
public record IssuedDocumentSummary(
        String token,
        String templateId,
        LocalDateTime createdAt,
        boolean sealed
) {
    public static IssuedDocumentSummary from(OfficialDocument document) {
        return new IssuedDocumentSummary(
                document.getShareToken(),
                document.getTemplateId(),
                document.getCreatedAt(),
                document.getSealContentType() != null);
    }
}
