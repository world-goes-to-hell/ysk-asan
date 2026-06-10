# M9 — 연락처 CSV 가져오기 (일괄 등록)

> 작성 2026-06-10(수). 상태: **구현 완료 — 검증 통과**. Tier 2 마지막 — 기존 엑셀 데이터 일괄 이관(엑셀 대체 완성).

## 검증 결과
- 백엔드 테스트 86개 PASS(신규 7: UTF8-BOM/CP949 폴백/오류행 400+row/헤더/쿼팅·가드 라운드트립/401/403/빈파일).
- 브라우저 E2E **라운드트립**: 등록 2건 → 내보내기 → 전체 삭제(0건) → 가져오기 → 2건 복원(부서 탭·변경 이력 포함) ✓
- 오류 모달: 불량 2행 파일 → "아무것도 등록하지 않았습니다" + 행별 메시지(2행 이메일 형식/3행 부서 필수), 기존 데이터 무손상 ✓
- 보안 리뷰: CRITICAL/HIGH 0 — MEDIUM 1건(IOException 메시지 노출, CWE-209) 반영(서버 로그만 상세).
  LOW 2건(Content-Type 미검증·거대 단일 행)은 2MB 한도/파싱 실패 처리로 상쇄 — 수용.

## 요구사항
- CSV 파일 업로드로 연락처 **일괄 등록** — M7 내보내기 포맷(`부서,이름,이메일`)과 라운드트립.
- 행 단위 검증 + **행 번호가 포함된 오류 리포팅**.

## 핵심 설계 결정

### 인코딩 (한국 Excel 현실 대응)
- BOM(EF BB BF) 있으면 UTF-8.
- BOM 없으면 **UTF-8 strict 디코드 시도 → 실패 시 CP949(MS949)** — 한국어 Excel의
  레거시 "CSV(쉼표로 분리)" 저장이 CP949 라서 이 폴백이 없으면 한글이 전부 깨진다.

### 파싱/검증
- **Apache Commons CSV**(battle-tested) — RFC 4180 쿼팅/이중따옴표/필드 내 개행 정확 처리.
  직접 파서 구현 금지(연구·재사용 원칙).
- 헤더 행 필수: `부서,이름,이메일` 정확 일치(공백 허용) — 컬럼 밀림 오등록 방지.
- 행 검증: 기존 `ContactRequest`의 Bean Validation 재사용(프로그래매틱 `Validator`) —
  정책 단일화(NotBlank/길이/이메일 형식).
- M7 수식 인젝션 가드 라운드트립: `'` + (`=+-@\t\r|`) 시작 필드는 가드용 `'` 제거.
- **all-or-nothing**: 한 행이라도 오류면 아무것도 등록하지 않고 400 + 오류 목록(행 번호) —
  부분 성공보다 예측 가능(사내 사용자 UX).
- 한도: 최대 5,000행, 파일 2MB(`spring.servlet.multipart.max-file-size`). 중복(이메일/이름)은
  기존 정책대로 허용(전역 공유·중복 허용 — Contact 주석 참조).

### API
- `POST /api/contacts/import` (multipart `file`) — 인증 필수, **CSRF 적용**(apiFetch가 FormData도
  X-XSRF-TOKEN 첨부함 — client.js 기존 지원).
- 성공: `{"imported": N}`. 검증 실패: 400 `{"message": "...", "errors": [{"row": 3, "message": "..."}]}`
  → `CsvImportException` + GlobalExceptionHandler 매핑.
- 구현: `ContactImportService` 분리(작은 파일 원칙 — ContactService 비대 방지).

### 프론트
- "CSV 가져오기" 버튼(내보내기 옆) → 숨김 file input → 업로드.
- 성공: 토스트 "N건을 등록했습니다" + 목록 재조회(`useContacts`에 `reload` 노출 추가).
- 실패: 오류 행 목록 모달(최대 10건 + "외 N건", modal css 재사용).

## 테스트 (ContactControllerTest)
1. UTF-8(BOM) 정상 2행 → 200 {imported:2} + 목록 반영
2. **CP949 인코딩** 한글 → 정상 등록(폴백 검증)
3. 잘못된 이메일 행 → 400 + errors[].row + **아무것도 등록 안 됨**(all-or-nothing)
4. 헤더 불일치 → 400
5. 쿼팅 필드(쉼표/이중따옴표) 라운드트립 + 가드 `'` 제거
6. 미인증 401 / CSRF 누락 403
7. 빈 파일 400

## 검증
`./gradlew test` + 프론트 빌드/Vitest + 브라우저 E2E **라운드트립**(등록→내보내기→전체삭제→가져오기→복원 확인).

## 배포
- 의존성 추가: `org.apache.commons:commons-csv`. 스키마 변경 없음(DB_DDL_AUTO 불필요).

## 범위 밖
- .xlsx 네이티브 파싱(CSV로 충분), 중복 감지/병합, 부분 성공(skip-on-error) 모드.
