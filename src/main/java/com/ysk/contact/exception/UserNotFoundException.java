package com.ysk.contact.exception;

/**
 * 요청한 회원이 존재하지 않을 때 발생한다. {@code GlobalExceptionHandler} 에서 404 로 매핑된다.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long id) {
        super("회원을 찾을 수 없습니다: id=" + id);
    }
}
