package com.ysk.contact.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 발급된 공문(직인 URL). 직원이 양식에 내용을 채워 발급하면 shareToken 으로 공개 열람 URL 이
 * 만들어지고, 직인 보유자가 비밀번호 확인 후 직인 이미지를 날인한다.
 *
 * <p>보안: 비밀번호는 bcrypt 해시만 저장. 공개 엔드포인트이므로 비밀번호 연속 오류가
 * 한도에 도달하면 잠그고 존재 자체를 숨긴다(재발급 필요).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "official_document", indexes = {
        @Index(name = "idx_document_token", columnList = "share_token", unique = true)
})
public class OfficialDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 공개 열람 URL 토큰(256bit SecureRandom → base64url 43자). */
    @Column(name = "share_token", nullable = false, unique = true, length = 64)
    private String shareToken;

    @Column(nullable = false, length = 50)
    private String templateId;

    /** 양식 입력값(Map → JSON). 양식 구조는 프론트 하드코딩이라 서버는 통째로 보관만 한다. */
    @Lob
    @Column(nullable = false, columnDefinition = "text")
    private String fieldsJson;

    @Column(nullable = false)
    private String passwordHash;

    /** 직인 이미지(png/jpeg ≤1MB). 파일시스템 대신 DB 보관 — 볼륨/백업 단순화. */
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(columnDefinition = "mediumblob")
    private byte[] sealImage;

    @Column(length = 50)
    private String sealContentType;

    @Column(nullable = false, columnDefinition = "int default 0")
    private int failedAttempts;

    @CreatedBy
    @Column(updatable = false, length = 30)
    private String createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public boolean isLocked(int maxFailures) {
        return failedAttempts >= maxFailures;
    }

    public void recordPasswordFailure() {
        this.failedAttempts++;
    }

    public void resetPasswordFailures() {
        this.failedAttempts = 0;
    }

    public void attachSeal(byte[] image, String contentType) {
        this.sealImage = image;
        this.sealContentType = contentType;
    }
}
