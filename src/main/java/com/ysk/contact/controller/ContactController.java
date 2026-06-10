package com.ysk.contact.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ysk.contact.dto.ContactRequest;
import com.ysk.contact.dto.ContactResponse;
import com.ysk.contact.service.ContactService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @GetMapping
    public ResponseEntity<List<ContactResponse>> list(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(contactService.list(department, q));
    }

    @GetMapping("/departments")
    public ResponseEntity<List<String>> departments() {
        return ResponseEntity.ok(contactService.departments());
    }

    /** 현재 필터(부서/검색어)를 적용한 연락처 CSV 다운로드. 파일명은 ASCII 안전(contacts-YYYYMMDD.csv). */
    @GetMapping("/export")
    public ResponseEntity<String> export(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String q) {
        String filename = "contacts-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv";
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(contactService.exportCsv(department, q));
    }

    @PostMapping
    public ResponseEntity<ContactResponse> create(@Valid @RequestBody ContactRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contactService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ContactResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody ContactRequest request) {
        return ResponseEntity.ok(contactService.update(id, request));
    }

    /** 선택 항목 일괄 삭제. {@code ids} 파라미터는 필수(누락 시 400). 응답은 실제 삭제 개수. */
    @DeleteMapping
    public ResponseEntity<Map<String, Integer>> delete(@RequestParam List<Long> ids) {
        int deleted = contactService.deleteByIds(ids);
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }
}
