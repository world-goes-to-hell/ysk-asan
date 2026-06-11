package com.ysk.contact.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 공문 발급(직인 URL 생성) 요청. fields 구조는 양식(템플릿)별로 다르며 프론트에 정의된다. */
public record DocumentIssueRequest(
        @NotBlank(message = "양식은 필수입니다.")
        String templateId,

        @NotNull(message = "입력값은 필수입니다.")
        Map<String, String> fields,

        @NotBlank(message = "열람 비밀번호는 필수입니다.")
        @Size(min = 4, max = 72, message = "열람 비밀번호는 4~72자여야 합니다.")
        String password
) {}
