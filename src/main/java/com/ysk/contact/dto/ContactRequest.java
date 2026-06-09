package com.ysk.contact.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 연락처 추가/수정 요청. 추가와 수정의 입력 필드가 동일하므로 하나의 record 를 공유한다.
 */
public record ContactRequest(
        @NotBlank(message = "부서는 필수입니다.")
        @Size(max = 100, message = "부서는 100자 이하여야 합니다.")
        String department,

        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 100, message = "이름은 100자 이하여야 합니다.")
        String name,

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 255, message = "이메일은 255자 이하여야 합니다.")
        String email
) {}
