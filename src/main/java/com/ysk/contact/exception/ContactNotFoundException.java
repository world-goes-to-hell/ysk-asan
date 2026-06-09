package com.ysk.contact.exception;

/**
 * 요청한 연락처가 존재하지 않을 때 발생한다. {@code GlobalExceptionHandler} 에서 404 로 매핑된다.
 */
public class ContactNotFoundException extends RuntimeException {

    public ContactNotFoundException(Long id) {
        super("연락처를 찾을 수 없습니다: id=" + id);
    }
}
