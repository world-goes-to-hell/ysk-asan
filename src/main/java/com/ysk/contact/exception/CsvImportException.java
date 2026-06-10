package com.ysk.contact.exception;

import java.util.List;

import com.ysk.contact.dto.CsvRowError;

/**
 * CSV 가져오기 검증 실패. all-or-nothing 정책에 따라 한 행이라도 오류면 전체를 거부하며,
 * {@code GlobalExceptionHandler} 가 400 + 행별 오류 목록으로 매핑한다.
 */
public class CsvImportException extends RuntimeException {

    private final transient List<CsvRowError> errors;

    public CsvImportException(String message, List<CsvRowError> errors) {
        super(message);
        this.errors = List.copyOf(errors);
    }

    public CsvImportException(String message) {
        this(message, List.of());
    }

    public List<CsvRowError> getErrors() {
        return errors;
    }
}
