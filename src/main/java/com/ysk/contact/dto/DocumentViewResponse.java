package com.ysk.contact.dto;

import java.util.Base64;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ysk.contact.entity.OfficialDocument;

/**
 * 공문 열람 응답. 직인 이미지는 별도 URL(비밀번호 노출 위험) 대신 base64 로 동봉한다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DocumentViewResponse(
        String templateId,
        Map<String, String> fields,
        String sealImageBase64,
        String sealContentType
) {
    public static DocumentViewResponse of(OfficialDocument document, Map<String, String> fields) {
        byte[] seal = document.getSealImage();
        return new DocumentViewResponse(
                document.getTemplateId(),
                fields,
                seal == null ? null : Base64.getEncoder().encodeToString(seal),
                seal == null ? null : document.getSealContentType());
    }
}
