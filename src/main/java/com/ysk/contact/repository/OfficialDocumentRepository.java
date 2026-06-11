package com.ysk.contact.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ysk.contact.entity.OfficialDocument;

import jakarta.persistence.LockModeType;

public interface OfficialDocumentRepository extends JpaRepository<OfficialDocument, Long> {

    Optional<OfficialDocument> findByShareToken(String shareToken);

    java.util.List<OfficialDocument> findByCreatedByOrderByCreatedAtDesc(String createdBy);

    /**
     * 비밀번호 검증용 조회 — 행 잠금(PESSIMISTIC_WRITE)으로 동시 시도 시
     * failedAttempts 증가의 lost-update 를 막는다(잠금 카운터 정확성).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM OfficialDocument d WHERE d.shareToken = :token")
    Optional<OfficialDocument> findByShareTokenForUpdate(@Param("token") String token);
}
