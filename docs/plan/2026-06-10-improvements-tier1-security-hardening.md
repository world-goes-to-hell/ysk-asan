# Tier 1 — 빠른 보안·안정성 강화 (개선 감사 1차)

> 6관점 코드 감사(보안/기능/품질/운영/성능/UX) 결과 중, **회귀 위험이 낮고 효과 즉시 나는** 항목 묶음.
> 작성일 2026-06-10(수). 사내 소수 사용자 + PII(병원 직원 연락처) + HTTPS 프록시 뒤 운영 전제.
> 한 항목씩 구현 → 검증 → 커밋한다.

## 항목 및 진행상황

- [ ] **T1-1. 에러 응답 하드닝** — 내부정보/사용자명 노출 차단 + 미처리 예외 catch-all
  - `GlobalExceptionHandler` → `ResponseEntityExceptionHandler` 상속(프레임워크 4xx 보존),
    `@ExceptionHandler(Exception.class)` 추가(서버 로그만 상세, 클라엔 일반화 500), SLF4J 로깅 도입.
  - `UserService.findByUsername` 예외 메시지에서 username 제거.
  - 주의: 검증 응답은 기존 `{message}` 형태 유지(프론트 `apiFetch` 계약 + 테스트 `$.message`).
- [ ] **T1-2. 보안 응답 헤더** — `SecurityConfig`에 `.headers()` 추가(HSTS, X-Frame-Options DENY, X-Content-Type-Options nosniff). HSTS는 prod(secure)에서만 의미.
- [ ] **T1-3. 세션/토큰 수명** — prod 세션 타임아웃 명시(예 8h), Remember-Me 14일→7일, 쿠키 SameSite.
- [ ] **T1-4. 검색 컬럼 인덱스** — `Contact` 엔티티 `@Index`(department, name, email). LIKE '%..%' 한계는 별도 문서화.
- [ ] **T1-5. 폼 HTML 검증 속성** — `RegisterForm`/`ContactAddForm` input에 `type/required/minLength/maxLength`(접근성 + 브라우저 검증).

## 검증 방법
- 백엔드: `./gradlew test`(SSH 터널 ON, `ysk_asan_test` 격리). 기존 46개 회귀 없음 확인.
- 수동 앱 검증 필요 시 반드시 `--spring.profiles.active=local,localtest`(실 `ysk_asan` 오염 방지).
- 프론트: `npm --prefix frontend run build` + 필요 시 Vitest.

## 범위 밖(다음 Tier)
- 회원가입 제한 정책(사용자 결정 필요), 변경 이력(createdBy/updatedBy), CSV 내보내기/가져오기, 비밀번호 변경/프로필 — Tier 2.
- Flyway, Actuator+헬스체크, Dockerfile 비루트, 로그 정책 — Tier 3.
