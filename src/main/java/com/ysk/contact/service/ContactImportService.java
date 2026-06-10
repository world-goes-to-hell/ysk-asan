package com.ysk.contact.service;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.ysk.contact.dto.ContactRequest;
import com.ysk.contact.dto.CsvRowError;
import com.ysk.contact.entity.Contact;
import com.ysk.contact.exception.CsvImportException;
import com.ysk.contact.repository.ContactRepository;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

/**
 * CSV 일괄 가져오기. M7 내보내기 포맷(헤더 {@code 부서,이름,이메일})과 라운드트립된다.
 *
 * <p>정책:
 * <ul>
 *   <li><b>all-or-nothing</b> — 한 행이라도 오류면 아무것도 등록하지 않고 행별 오류를 반환.</li>
 *   <li>인코딩 — BOM 있으면 UTF-8, 없으면 UTF-8 strict 시도 후 실패 시 CP949 폴백
 *       (한국어 Excel 레거시 "CSV(쉼표로 분리)" 저장이 CP949).</li>
 *   <li>행 검증은 {@link ContactRequest} 의 Bean Validation 을 재사용해 등록 API 와 정책 단일화.</li>
 *   <li>내보내기의 수식 인젝션 가드({@code '} prefix)는 원본 값으로 복원.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ContactImportService {

    private static final Logger log = LoggerFactory.getLogger(ContactImportService.class);

    private static final List<String> EXPECTED_HEADER = List.of("부서", "이름", "이메일");
    private static final int MAX_ROWS = 5_000;
    private static final String GUARD_PREFIXES = "=+-@\t\r|";

    private final ContactRepository contactRepository;
    private final Validator validator;

    @Transactional
    public int importCsv(MultipartFile file) {
        String content = decode(readBytes(file));

        List<CsvRowError> errors = new ArrayList<>();
        List<Contact> contacts = new ArrayList<>();

        try (CSVParser parser = CSVParser.parse(new StringReader(content), CSVFormat.DEFAULT)) {
            boolean headerSeen = false;
            for (CSVRecord record : parser) {
                if (!headerSeen) {
                    validateHeader(record);
                    headerSeen = true;
                    continue;
                }
                if (contacts.size() + errors.size() >= MAX_ROWS) {
                    throw new CsvImportException("한 번에 최대 " + MAX_ROWS + "행까지 가져올 수 있습니다.");
                }
                parseRow(record, contacts, errors);
            }
            if (!headerSeen) {
                throw new CsvImportException("파일이 비어 있습니다.");
            }
        } catch (IOException e) {
            // 내부 상세(라이브러리 메시지/경로)는 서버 로그만 — 클라이언트엔 일반화 메시지(CWE-209).
            log.warn("CSV 파싱 오류", e);
            throw new CsvImportException("CSV 파싱에 실패했습니다. 파일 형식을 확인하세요.");
        }

        if (!errors.isEmpty()) {
            throw new CsvImportException(
                    "가져오기 실패 — " + errors.size() + "개 행에 오류가 있어 아무것도 등록하지 않았습니다.", errors);
        }
        if (contacts.isEmpty()) {
            throw new CsvImportException("등록할 데이터 행이 없습니다.");
        }

        contactRepository.saveAll(contacts);
        return contacts.size();
    }

    private void parseRow(CSVRecord record, List<Contact> contacts, List<CsvRowError> errors) {
        if (record.size() != EXPECTED_HEADER.size()) {
            errors.add(new CsvRowError(record.getRecordNumber(),
                    "열 개수가 3개(부서,이름,이메일)가 아닙니다."));
            return;
        }
        ContactRequest request = new ContactRequest(
                unguard(record.get(0).trim()),
                unguard(record.get(1).trim()),
                unguard(record.get(2).trim()));

        Set<ConstraintViolation<ContactRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .sorted()
                    .reduce((a, b) -> a + " " + b)
                    .orElse("잘못된 값입니다.");
            errors.add(new CsvRowError(record.getRecordNumber(), message));
            return;
        }
        contacts.add(Contact.builder()
                .department(request.department())
                .name(request.name())
                .email(request.email())
                .build());
    }

    private void validateHeader(CSVRecord header) {
        boolean matches = header.size() == EXPECTED_HEADER.size();
        for (int i = 0; matches && i < EXPECTED_HEADER.size(); i++) {
            matches = EXPECTED_HEADER.get(i).equals(header.get(i).trim());
        }
        if (!matches) {
            throw new CsvImportException(
                    "첫 행은 헤더 '부서,이름,이메일' 이어야 합니다(내보내기 파일 형식과 동일).");
        }
    }

    /** 내보내기의 수식 인젝션 가드(') 를 원본 값으로 복원한다. */
    private static String unguard(String value) {
        if (value.length() >= 2 && value.charAt(0) == '\''
                && GUARD_PREFIXES.indexOf(value.charAt(1)) >= 0) {
            return value.substring(1);
        }
        return value;
    }

    private static byte[] readBytes(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length == 0) {
                throw new CsvImportException("파일이 비어 있습니다.");
            }
            return bytes;
        } catch (IOException e) {
            log.warn("업로드 파일 읽기 오류", e);
            throw new CsvImportException("파일을 읽지 못했습니다. 다시 시도해 주세요.");
        }
    }

    /** BOM → UTF-8, 그 외 UTF-8 strict 시도 후 실패 시 CP949(한국어 Excel 레거시 저장) 폴백. */
    private static String decode(byte[] bytes) {
        if (bytes.length >= 3
                && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
            return new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        }
        try {
            CharsetDecoder strict = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            return strict.decode(ByteBuffer.wrap(bytes)).toString();
        } catch (java.nio.charset.CharacterCodingException e) {
            return new String(bytes, Charset.forName("MS949"));
        }
    }
}
