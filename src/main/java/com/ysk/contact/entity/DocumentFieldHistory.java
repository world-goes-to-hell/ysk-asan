package com.ysk.contact.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 공문 입력값 이력(자동완성 풀). 양식·필드별로 값을 1행만 유지(유니크)하고
 * 재사용 시 lastUsedAt 만 갱신해 최근 사용순 제안을 만든다.
 * 전역 공유(모든 사용자 누적) — 짧은 단일라인 값만 저장되므로 본문류 내용은 들어오지 않는다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "document_field_history", uniqueConstraints = {
        @UniqueConstraint(name = "uk_field_history",
                columnNames = { "template_id", "field_key", "value" })
})
public class DocumentFieldHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_id", nullable = false, length = 50)
    private String templateId;

    @Column(name = "field_key", nullable = false, length = 50)
    private String fieldKey;

    @Column(nullable = false, length = 120)
    private String value;

    @Column(nullable = false)
    private LocalDateTime lastUsedAt;

    public static DocumentFieldHistory of(String templateId, String fieldKey, String value) {
        return DocumentFieldHistory.builder()
                .templateId(templateId)
                .fieldKey(fieldKey)
                .value(value)
                .lastUsedAt(LocalDateTime.now())
                .build();
    }

    /** 값 재사용 — 최근 사용 시각 갱신(정렬 상위로). */
    public void touch() {
        this.lastUsedAt = LocalDateTime.now();
    }
}
