package com.ysk.contact.entity;

/**
 * 회원 권한. ADMIN 은 회원 관리(승인/잠금 해제/권한 변경)가 가능하다.
 * Spring Security 권한으로는 {@code ROLE_} 접두사를 붙여 매핑된다.
 */
public enum UserRole {
    ADMIN,
    USER
}
