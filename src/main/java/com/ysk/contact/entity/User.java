package com.ysk.contact.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원. 가입은 누구나 가능하지만 관리자 승인({@code approved}) 전에는 로그인할 수 없고,
 * 비밀번호 연속 오류가 한도에 도달하면 잠긴다({@code locked} — 관리자가 해제).
 * 상태 변경은 의도된 도메인 메서드로만 노출한다(@Setter 금지 — Contact 패턴).
 *
 * <p>마이그레이션 주의: 신규 컬럼의 DDL default 는 {@code ddl-auto=update} 가 기존 행이 있는
 * 테이블에 컬럼을 추가할 때의 backfill 값이다. {@code approved} 의 default 가 true 인 이유는
 * 승인제 도입 전 기존 계정을 전부 승인 상태로 전환(grandfather)하기 위함이며, JPA INSERT 는
 * 항상 명시값을 쓰므로 신규 가입은 코드의 {@code approved=false} 가 적용된다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20) default 'USER'")
    private UserRole role;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean approved;

    @Column(name = "account_locked", nullable = false, columnDefinition = "boolean default false")
    private boolean locked;

    @Column(nullable = false, columnDefinition = "int default 0")
    private int failedLoginAttempts;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /** 관리자 승인 — 이후 로그인 가능. */
    public void approve() {
        this.approved = true;
    }

    /** 잠금 해제 + 실패 카운터 초기화(관리자 액션). */
    public void unlock() {
        this.locked = false;
        this.failedLoginAttempts = 0;
    }

    /**
     * 로그인 실패를 기록하고, 누적 실패가 {@code maxFailures} 에 도달하면 계정을 잠근다.
     *
     * @return 이번 기록으로 계정이 잠겼으면 true
     */
    public boolean recordLoginFailure(int maxFailures) {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= maxFailures) {
            this.locked = true;
            return true;
        }
        return false;
    }

    /** 로그인 성공 시 실패 카운터 초기화. */
    public void resetLoginFailures() {
        this.failedLoginAttempts = 0;
    }

    public void changeRole(UserRole role) {
        this.role = role;
    }
}
