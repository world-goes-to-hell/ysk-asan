package com.ysk.contact.service;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ysk.contact.dto.DocumentIssueRequest;
import com.ysk.contact.dto.DocumentViewResponse;
import com.ysk.contact.entity.DocumentFieldHistory;
import com.ysk.contact.entity.OfficialDocument;
import com.ysk.contact.exception.DocumentNotFoundException;
import com.ysk.contact.exception.DocumentPasswordException;
import com.ysk.contact.repository.DocumentFieldHistoryRepository;
import com.ysk.contact.repository.OfficialDocumentRepository;

import lombok.RequiredArgsConstructor;

/**
 * 공문 발급/공개 열람/직인 날인.
 *
 * <p>공개 엔드포인트 보안 원칙:
 * <ul>
 *   <li>토큰 미존재와 잠김을 동일하게 404 처리(존재 비노출).</li>
 *   <li>비밀번호 연속 오류 {@value #MAX_PASSWORD_FAILURES}회 → 잠금(재발급 필요), 성공 시 카운터 리셋.</li>
 *   <li>검증 후 세션/상태를 만들지 않음 — 매 요청 비밀번호 동봉(무상태).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class OfficialDocumentService {

    /** 프론트 템플릿 레지스트리(frontend/src/documents/templates.jsx)와 반드시 동기화. */
    private static final Set<String> KNOWN_TEMPLATES =
            Set.of("general-official", "employment-cert", "advance-payment");

    private static final int MAX_PASSWORD_FAILURES = 10;
    private static final int MAX_FIELDS = 30;
    private static final int MAX_FIELDS_JSON_BYTES = 10_000;
    private static final long MAX_SEAL_BYTES = 1024 * 1024; // 1MB
    private static final Set<String> SEAL_CONTENT_TYPES = Set.of("image/png", "image/jpeg");

    /** 자동완성 이력에 저장할 값의 최대 길이(본문류 제외 휴리스틱과 짝). */
    private static final int MAX_HISTORY_VALUE_LENGTH = 100;
    private static final int MAX_HISTORY_SUGGESTIONS_PER_FIELD = 20;

    private final OfficialDocumentRepository documentRepository;
    private final DocumentFieldHistoryRepository fieldHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public String issue(DocumentIssueRequest request) {
        if (!KNOWN_TEMPLATES.contains(request.templateId())) {
            throw new IllegalArgumentException("알 수 없는 양식입니다: " + request.templateId());
        }
        if (request.fields().size() > MAX_FIELDS) {
            throw new IllegalArgumentException("입력 항목이 너무 많습니다.");
        }
        String fieldsJson = toJson(request.fields());
        if (fieldsJson.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_FIELDS_JSON_BYTES) {
            throw new IllegalArgumentException("입력 내용이 너무 깁니다.");
        }

        OfficialDocument document = OfficialDocument.builder()
                .shareToken(generateToken())
                .templateId(request.templateId())
                .fieldsJson(fieldsJson)
                .passwordHash(passwordEncoder.encode(request.password()))
                .build();
        documentRepository.save(document);
        recordFieldHistory(request.templateId(), request.fields());
        return document.getShareToken();
    }

    /**
     * 자동완성 이력 기록. 본문류(개행 포함·100자 초과)와 빈 값은 제외하고,
     * 같은 값은 1행만 유지하며 lastUsedAt 만 갱신한다(최근 사용순 제안).
     */
    private void recordFieldHistory(String templateId, Map<String, String> fields) {
        fields.forEach((key, raw) -> {
            if (key == null || key.length() > 50 || raw == null) {
                return;
            }
            String value = raw.trim();
            if (value.isEmpty() || value.length() > MAX_HISTORY_VALUE_LENGTH
                    || value.contains("\n") || value.contains("\r")) {
                return;
            }
            fieldHistoryRepository.findByTemplateIdAndFieldKeyAndValue(templateId, key, value)
                    .ifPresentOrElse(DocumentFieldHistory::touch,
                            () -> fieldHistoryRepository.save(
                                    DocumentFieldHistory.of(templateId, key, value)));
        });
    }

    /** 양식별 자동완성 이력 — {필드키: [값(최근 사용순, 상위 20)]}. */
    @Transactional(readOnly = true)
    public Map<String, java.util.List<String>> fieldHistory(String templateId) {
        if (!KNOWN_TEMPLATES.contains(templateId)) {
            throw new IllegalArgumentException("알 수 없는 양식입니다: " + templateId);
        }
        Map<String, java.util.List<String>> out = new java.util.LinkedHashMap<>();
        for (DocumentFieldHistory history
                : fieldHistoryRepository.findTop300ByTemplateIdOrderByLastUsedAtDescIdDesc(templateId)) {
            java.util.List<String> values =
                    out.computeIfAbsent(history.getFieldKey(), k -> new java.util.ArrayList<>());
            if (values.size() < MAX_HISTORY_SUGGESTIONS_PER_FIELD) {
                values.add(history.getValue());
            }
        }
        return out;
    }

    /** 발급자 본인의 공문 목록(최신순). */
    @Transactional(readOnly = true)
    public java.util.List<com.ysk.contact.dto.IssuedDocumentSummary> listMine(String username) {
        return documentRepository.findByCreatedByOrderByCreatedAtDesc(username).stream()
                .map(com.ysk.contact.dto.IssuedDocumentSummary::from)
                .toList();
    }

    /**
     * 발급자 본인 열람 — 비밀번호 불필요(로그인 + 소유 확인). 타인 문서는 404(존재 비노출).
     * 비밀번호 오류 잠금과 무관하게 발급자는 항상 열람할 수 있다.
     */
    @Transactional(readOnly = true)
    public DocumentViewResponse issuerView(String token, String username) {
        OfficialDocument document = documentRepository.findByShareToken(token)
                .filter(d -> username.equals(d.getCreatedBy()))
                .orElseThrow(DocumentNotFoundException::new);
        return DocumentViewResponse.of(document, fromJson(document.getFieldsJson()));
    }

    // noRollbackFor: 비밀번호 오류 시에도 실패 카운트 증가가 커밋되어야 잠금이 동작한다.
    @Transactional(noRollbackFor = DocumentPasswordException.class)
    public DocumentViewResponse view(String token, String password) {
        OfficialDocument document = findUsable(token);
        verifyPassword(document, password);
        return DocumentViewResponse.of(document, fromJson(document.getFieldsJson()));
    }

    @Transactional(noRollbackFor = DocumentPasswordException.class)
    public void attachSeal(String token, String password, MultipartFile file) {
        OfficialDocument document = findUsable(token);
        verifyPassword(document, password);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("직인 이미지 파일을 선택하세요.");
        }
        if (file.getSize() > MAX_SEAL_BYTES) {
            throw new IllegalArgumentException("직인 이미지는 1MB 이하여야 합니다.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !SEAL_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("직인 이미지는 PNG 또는 JPEG 만 업로드할 수 있습니다.");
        }
        try {
            byte[] bytes = file.getBytes();
            // Content-Type 은 클라이언트 선언값(스푸핑 가능) — 매직 바이트로 실제 이미지인지 확인.
            validateImageMagicBytes(bytes, contentType);
            document.attachSeal(bytes, contentType);
        } catch (IOException e) {
            throw new IllegalArgumentException("파일을 읽지 못했습니다. 다시 시도해 주세요.");
        }
    }

    private static final java.util.Map<String, byte[]> MAGIC_BYTES = java.util.Map.of(
            "image/png", new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47 },
            "image/jpeg", new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF });

    private static void validateImageMagicBytes(byte[] data, String contentType) {
        byte[] magic = MAGIC_BYTES.get(contentType);
        for (int i = 0; magic != null && i < magic.length; i++) {
            if (i >= data.length || data[i] != magic[i]) {
                throw new IllegalArgumentException("직인 이미지 파일이 손상되었거나 형식이 올바르지 않습니다.");
            }
        }
    }

    /**
     * 토큰 미존재와 잠김을 동일하게 404 처리(존재 비노출).
     * 행 잠금 조회로 동시 비밀번호 시도 간 실패 카운터 경쟁을 막는다.
     */
    private OfficialDocument findUsable(String token) {
        OfficialDocument document = documentRepository.findByShareTokenForUpdate(token)
                .orElseThrow(DocumentNotFoundException::new);
        if (document.isLocked(MAX_PASSWORD_FAILURES)) {
            throw new DocumentNotFoundException();
        }
        return document;
    }

    private void verifyPassword(OfficialDocument document, String password) {
        if (!passwordEncoder.matches(password, document.getPasswordHash())) {
            document.recordPasswordFailure();
            throw new DocumentPasswordException();
        }
        if (document.getFailedAttempts() > 0) {
            document.resetPasswordFailures();
        }
    }

    /** 256bit SecureRandom → base64url 43자(패딩 없음). URL 추측 불가 수준의 엔트로피. */
    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String toJson(Map<String, String> fields) {
        try {
            return objectMapper.writeValueAsString(fields);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("입력값을 처리하지 못했습니다.");
        }
    }

    private Map<String, String> fromJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (JsonProcessingException e) {
            // 저장 시 직렬화를 통과한 값이므로 도달 불가에 가깝다 — 방어적 처리.
            throw new IllegalStateException("저장된 공문 데이터를 읽지 못했습니다.");
        }
    }
}
