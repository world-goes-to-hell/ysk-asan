package com.ysk.contact.config;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.ysk.contact.exception.ContactNotFoundException;
import com.ysk.contact.exception.UserNotFoundException;

/**
 * 전역 예외 처리. {@link ResponseEntityExceptionHandler} 를 상속해 Spring MVC 표준 예외
 * (잘못된 JSON·메서드 미지원·필수 파라미터 누락 등)의 올바른 4xx 처리를 보존하면서,
 * 도메인 예외와 미처리 예외를 일관된 {@code {"message": ...}} 형태로 응답한다.
 *
 * <p>보안 원칙: 내부 메시지/스택트레이스는 서버 로그에만 남기고, 클라이언트에는 일반화된
 * 메시지만 반환해 정보 노출을 막는다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Bean Validation(@Valid) 실패. 기본 ProblemDetail 대신 첫 필드 오류를
     * {@code {"message": "필드: 사유"}} 형태로 반환해 프론트 apiFetch 계약을 유지한다.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("잘못된 요청입니다.");
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArg(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(ContactNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ContactNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUserNotFound(UserNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrity(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "이미 사용 중인 값입니다."));
    }

    /**
     * 위에서 명시적으로 처리하지 않은 모든 예외의 최종 방어선. 상세는 서버 로그에만 남기고
     * 클라이언트에는 내부 정보가 새지 않도록 일반화된 500 메시지만 반환한다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpected(Exception e) {
        log.error("처리되지 않은 예외 발생", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "서버 내부 오류가 발생했습니다."));
    }
}
