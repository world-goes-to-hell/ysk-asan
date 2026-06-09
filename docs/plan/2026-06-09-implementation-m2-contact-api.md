# M2: 연락처 도메인 API 구현 계획

> **For agentic workers:** TDD(RED→GREEN) Task 단위. 진행상황은 이 문서 하단 "진행 상황"에서 관리.

**Goal:** 엑셀 "수신처 관리"의 핵심 백엔드 — 연락처 CRUD + 부서 필터/검색 + 부서 탭(distinct) + 선택 일괄삭제 — 를 세션 인증 보호 하에 제공한다.

**Architecture:** M1과 동일한 레이어드(controller→service→repository→entity) + Spring Security 세션/CSRF. 통합/리포지토리 테스트는 SSH 터널 경유 `ysk_asan_test` 스키마(ddl create-drop), 서비스는 Mockito 단위.

---

## 전체 로드맵에서의 위치
- M1: 백엔드 부트스트랩 + 인증 ✅
- **M2 (이 문서)**: 연락처 도메인 API
- M3: 프론트 부트스트랩(Vite + AuthContext + 레이아웃 + 로그인)
- M4: 연락처 관리 화면 + 대시보드(placeholder)

**M2 완료 시 결과물:** `./gradlew bootRun`(local, 터널) → `contact` 테이블 자동 생성(ddl=update) → `/api/contacts` CRUD/검색/부서/일괄삭제 동작. 테스트 42개(M1 9 + M2 33) GREEN.

## 데이터 모델 / API

```
Contact (table: contact, 전역 공유, 이메일 중복 허용)
id(PK,IDENTITY) / department / name / email / createdAt(@PrePersist) / updatedAt(@PreUpdate)
```

| Method | Endpoint | 설명 | 상태 |
|--------|----------|------|------|
| GET | `/api/contacts?department=&q=` | 부서(exact) + name/email LIKE(대소문자무시) 필터, 둘 다 선택 | 200 |
| GET | `/api/contacts/departments` | distinct 부서 정렬 | 200 |
| POST | `/api/contacts` | 추가 | 201 |
| PUT | `/api/contacts/{id}` | 수정(미존재 404) | 200 |
| DELETE | `/api/contacts?ids=1,2,3` | 일괄삭제, `{deleted:N}` | 200 |

검증: department/name `@NotBlank @Size`, email `@NotBlank @Email @Size`.
인증: `/api/contacts/**` 인증 필요(미인증 401), 변경 메서드는 CSRF 토큰 필요(미토큰 403).

## 확정 설계 결정 (Plan 리뷰 반영)
1. **검색 JPQL — q-OR 그룹 괄호 필수**: 부서 필터가 q-OR 전체와 AND 결합되도록. 누락 시 타 부서 매치 누수 → 가드 테스트로 방어.
2. **빈 문자열 → null 정규화(서비스)**: `StringUtils.hasText` — `:q IS NULL`은 `""`에 false라 과필터 방지.
3. **엔티티 수정 = `update()` 도메인 메서드**: managed 엔티티 dirty checking(별도 save 없음). @Setter 미사용 규칙 준수.
4. **일괄삭제 = `findAllById → deleteAllInBatch → found.size()`**: 존재분만 삭제, 실제 개수 반환(부분 삭제 허용).
5. **빈/누락 ids → 400**: `@RequestParam List<Long> ids`(필수) 누락 시 자동 400; 빈 리스트는 서비스 `IllegalArgumentException`→400.
6. **404 격리**: `ContactNotFoundException` 전용 핸들러. me()는 직접 401 반환이라 무영향.
7. **미인증 401**: `/api/contacts`가 첫 `authenticated()` 경로 → `HttpStatusEntryPoint(401)`로 기본 403 대체(프론트 apiFetch 세션만료 처리용). M1 회귀 없음(`/api/auth/**` permitAll).
8. **DTO 반환**: `ContactService`는 `ContactResponse` 반환(M1 리뷰 MEDIUM-1).

## 생성/수정 파일
```
신규 entity/Contact.java, repository/ContactRepository.java
신규 dto/ContactRequest.java, dto/ContactResponse.java
신규 exception/ContactNotFoundException.java
신규 service/ContactService.java, controller/ContactController.java
수정 config/GlobalExceptionHandler.java(404), config/SecurityConfig.java(401)
수정 build.gradle(jacoco)
테스트 repository/ContactRepositoryTest(8), service/ContactServiceTest(10), controller/ContactControllerTest(15)
```

---

## 진행 상황 (2026-06-09)

- [x] **Task 1**: Contact 엔티티 + 검색/부서 ContactRepository — 커밋 `e1d9555`, ContactRepositoryTest 8 PASS(부서간 누수 가드 포함)
- [x] **Task 2**: DTO/예외 + ContactService — 커밋 `(2번째)`, ContactServiceTest 10 PASS(정규화·404·일괄삭제 부분/빈/null)
- [x] **Task 3**: 404 핸들러 + ContactController + 미인증 401 — 커밋 `84a77ec`, ContactControllerTest 15 PASS(인증401/CSRF403/검증400/404/검색/부서/일괄삭제)
- [x] **Task 4**: jacoco + 전체 검증 + 문서/메모리 — (이 커밋)

**✅ M2 완료.**

### 검증 증거
- `./gradlew test` **42개 PASS**(0 실패/0 에러): M1 9(회귀 없음) + M2 33.
- JaCoCo: ContactService/Controller/DTO/예외 **100%**, Contact 엔티티 89%, SecurityConfig 100%. 전체 LINE **95%**, INSTRUCTION 90%. (`./gradlew jacocoTestReport` → `build/reports/jacoco/test/html/index.html`)
- 실 MariaDB(`ysk_asan_test`)에서 `contact` 테이블 create-drop + 실제 SQL 실행으로 통합 검증. local profile 차이는 ddl-auto/DB명뿐(엔티티 DDL 동일).

### 수동 검증(선택 — 미수행)
통합테스트가 전체 컨텍스트+실 DB+필터체인을 검증해 생략. 원할 경우:
`./gradlew bootRun`(터널 ON) → 회원가입/로그인으로 세션+`XSRF-TOKEN` 쿠키 확보 → `POST /api/contacts`(201) → `GET ?department=영업` → `GET /departments` → `PUT /{id}` → `DELETE ?ids={id}`(`{deleted:1}`).

### 다음 (M3)
프론트 부트스트랩: Vite + 라우팅 + AuthContext + apiFetch(CSRF/401 처리) + 레이아웃/사이드바 + 로그인 화면. M2의 `/api/contacts` 계약을 그대로 소비.

### 미처리(보류) — M2 범위 밖, 향후
- 인증 보류 항목: me() 삭제사용자 401(HIGH-3), AuthService 분리(HIGH-4) — 사용자 결정으로 제외
- MEDIUM-2(통합테스트 터널 DB 의존 → CI 불안정), 향후 Testcontainers 또는 별도 CI DB 검토
