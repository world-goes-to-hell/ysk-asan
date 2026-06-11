package com.ysk.contact.exception;

/**
 * 공문이 없거나 잠긴 경우. 공개 토큰 엔드포인트에서 존재 여부를 숨기기 위해
 * 두 경우 모두 동일하게 404 로 매핑된다({@code GlobalExceptionHandler}).
 */
public class DocumentNotFoundException extends RuntimeException {

    public DocumentNotFoundException() {
        super("공문을 찾을 수 없습니다. 주소를 확인하거나 발급자에게 재발급을 요청하세요.");
    }
}
