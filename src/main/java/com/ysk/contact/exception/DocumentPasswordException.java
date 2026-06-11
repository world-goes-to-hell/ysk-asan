package com.ysk.contact.exception;

/**
 * 공문 열람 비밀번호 불일치. {@code GlobalExceptionHandler} 에서 401 로 매핑된다.
 */
public class DocumentPasswordException extends RuntimeException {

    public DocumentPasswordException() {
        super("비밀번호가 올바르지 않습니다.");
    }
}
