# M7 — 연락처 CSV 내보내기

> 작성 2026-06-10(수). 상태: **진행 중**. 기능개선 백로그 2번(엑셀 대체 목적 직결).

## 요구사항
- 연락처 목록을 CSV 파일로 다운로드(엑셀에서 바로 열기) — 백업/타 시스템 이관 용도.
- **현재 필터(부서 탭/검색어) 그대로 적용** — 화면에 보이는 것을 내보낸다.

## 설계

### 백엔드 — `GET /api/contacts/export?department=&q=`
- 기존 `ContactRepository.search` 재사용(목록과 동일 필터·정렬). 인증 필수(`/api/**` authenticated 기존 규칙).
- 컬럼: `부서,이름,이메일` (UI 테이블과 동일 — 향후 가져오기(M8 후보)와 라운드트립 호환).
- **Excel 한글 깨짐 방지: UTF-8 BOM**(`﻿`) 선두 부착 — 없으면 Windows Excel이 cp949로 읽어 한글 전부 깨짐.
- **RFC 4180 이스케이프**: 쉼표/따옴표/개행 포함 필드는 쿼팅 + 내부 따옴표 이중화.
- **CSV(수식) 인젝션 가드**: `= + - @ \t \r` 로 시작하는 셀은 작은따옴표(') prefix — 악의적 이름(`=HYPERLINK(...)`)이 엑셀에서 수식으로 실행되는 것 차단.
- 응답 헤더: `text/csv; charset=UTF-8`, `Content-Disposition: attachment; filename="contacts-YYYYMMDD.csv"`(ASCII 안전).

### 프론트
- 연락처 화면 헤더(검색창 옆) "CSV 내보내기" 버튼 — 현재 `activeDept`/`q`로 URL 구성.
- `fetch` blob 다운로드(apiFetch는 JSON 전용): 실패 시 토스트, 성공 시 브라우저 저장.

## 테스트 (ContactControllerTest 추가, @WithMockUser)
1. 미인증 401
2. 200 + BOM + 헤더행 + 데이터행, Content-Type/Disposition 확인
3. 부서 필터 적용(타 부서 미포함)
4. 이스케이프 + 수식 인젝션 가드(`=SUM(A1)` → `'=SUM(A1)`, `영업,1팀` → 쿼팅)

## 검증
`./gradlew test` + 프론트 빌드 + bootRun(localtest) 브라우저 다운로드 확인(엑셀 한글).

## 범위 밖
- CSV 가져오기(import) — 다음 후보(M8). 엑셀(.xlsx) 네이티브 포맷 — CSV로 충분.
