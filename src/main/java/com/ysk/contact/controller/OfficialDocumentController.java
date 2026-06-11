package com.ysk.contact.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ysk.contact.dto.DocumentIssueRequest;
import com.ysk.contact.dto.DocumentPasswordRequest;
import com.ysk.contact.dto.DocumentViewResponse;
import com.ysk.contact.dto.IssuedDocumentSummary;
import com.ysk.contact.service.OfficialDocumentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 공문 직인 URL. 발급은 로그인 사용자 전용, 열람/직인은 토큰+비밀번호 기반 공개
 * (SecurityConfig 에서 view/seal 만 permitAll — CSRF 는 그대로 적용).
 */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class OfficialDocumentController {

    private final OfficialDocumentService documentService;

    @PostMapping
    public ResponseEntity<Map<String, String>> issue(@Valid @RequestBody DocumentIssueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("token", documentService.issue(request)));
    }

    /** 발급자 본인의 공문 목록(로그인 전용). */
    @GetMapping("/mine")
    public ResponseEntity<List<IssuedDocumentSummary>> mine(Authentication authentication) {
        return ResponseEntity.ok(documentService.listMine(authentication.getName()));
    }

    /** 발급자 본인 열람(비밀번호 불필요 — 로그인 + 소유 확인). 인쇄용. */
    @GetMapping("/{token}/issuer-view")
    public ResponseEntity<DocumentViewResponse> issuerView(@PathVariable String token,
                                                           Authentication authentication) {
        return ResponseEntity.ok(documentService.issuerView(token, authentication.getName()));
    }

    @PostMapping("/{token}/view")
    public ResponseEntity<DocumentViewResponse> view(@PathVariable String token,
                                                     @Valid @RequestBody DocumentPasswordRequest request) {
        return ResponseEntity.ok(documentService.view(token, request.password()));
    }

    @PostMapping("/{token}/seal")
    public ResponseEntity<Map<String, Boolean>> attachSeal(@PathVariable String token,
                                                           @RequestParam("password") String password,
                                                           @RequestParam("file") MultipartFile file) {
        documentService.attachSeal(token, password, file);
        return ResponseEntity.ok(Map.of("sealed", true));
    }
}
