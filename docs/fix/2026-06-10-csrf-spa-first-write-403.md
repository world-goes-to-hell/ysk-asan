# SPA 첫 상태변경 요청이 403 (CSRF 쿠키 미발급)

- 날짜: 2026-06-10
- 영역: 백엔드 보안(Spring Security 6 CSRF) ↔ 프론트 apiFetch
- 관련 커밋: M3 통합 검증 중 발견 → CsrfCookieFilter 추가

## 증상
프론트(React) 부트스트랩 후 로그인까지는 정상인데, **첫 연락처 추가/수정/삭제(POST/PUT/DELETE)가 403 Forbidden**으로 막힘. 로그인 자체는 200, 조회(GET)는 정상.

bootRun + curl 통합 스모크로 실측:
```
GET  /api/auth/me   -> 401, 응답에 XSRF-TOKEN 쿠키 없음(!)
POST /api/auth/login-> 200, 여전히 XSRF-TOKEN 쿠키 없음
POST /api/contacts (X-XSRF-TOKEN: 빈값) -> 403
```

## 원인
Spring Security 6 의 CSRF 토큰은 **지연 로딩(deferred)** 이다. `CookieCsrfTokenRepository.withHttpOnlyFalse()` 는 요청 처리 중 `CsrfToken` 이 **실제로 읽힐 때만** `XSRF-TOKEN` 쿠키를 발급한다.

- `/api/auth/register`, `/api/auth/login` 은 CSRF **면제**(`ignoringRequestMatchers`)라 토큰을 읽지 않음 → 쿠키 미발급.
- `/api/auth/me` (GET) 도 핸들러가 토큰을 참조하지 않음 → 쿠키 미발급.

결과적으로 프론트는 `XSRF-TOKEN` 쿠키를 한 번도 받지 못하고, `apiFetch` 가 빈 `X-XSRF-TOKEN` 헤더를 보내 → CsrfFilter 가 거부(403). 401 이 아니라 **403** 이므로 `session-expired` 핸들링도 타지 않아 일반 에러로만 보임.

## 해결
`CsrfCookieFilter`(OncePerRequestFilter)를 추가해 **매 요청에서 토큰을 렌더링(`csrfToken.getToken()`)** → `CookieCsrfTokenRepository` 가 응답에 `XSRF-TOKEN` 쿠키를 싣도록 강제. 부트스트랩 `me()` GET 응답에 쿠키가 실려 프론트가 이후 쓰기에 헤더를 첨부할 수 있게 됨.

```java
// CsrfCookieFilter.java
CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
if (csrfToken != null) csrfToken.getToken();
filterChain.doFilter(request, response);
```
```java
// SecurityConfig: CsrfFilter 뒤에 등록
.addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class);
```

## 검증
재기동 후 동일 스모크:
```
GET  /api/auth/me   -> 401, XSRF-TOKEN 쿠키 발급됨 ✓ (UUID, len=36)
POST /api/auth/login-> 200
POST /api/contacts (X-XSRF-TOKEN: <쿠키값>) -> 201 ✓
```
백엔드 전체 테스트 42개 PASS(회귀 없음 — MockMvc `.with(csrf())` 는 토큰을 직접 주입하므로 무영향).

## 검토한 대안
- `.csrf(csrf -> csrf.spa())` (Spring Security 6.2 헬퍼): CsrfCookieFilter 를 자동 추가하지만 **XorCsrfTokenRequestAttributeHandler** 로 바뀌어, kanban-board 에서 검증된 plain 토큰(쿠키값 그대로 헤더 전송) 흐름과 달라질 수 있어 보류.
- `/api/auth/csrf` 프라이밍 엔드포인트: 프론트가 추가 호출해야 하고, 모든 보호 요청 전 선행 호출이 필요해 더 번거로움.
→ 최소 변경이면서 기존 plain 핸들러를 유지하는 **CsrfCookieFilter** 채택.

## 교훈 / 재발 방지
- SPA + Spring Security 6 CSRF 는 "쿠키가 실제로 언제 발급되는가"를 반드시 **실제 기동 + 요청**으로 확인할 것. 단위/MockMvc 테스트는 `.with(csrf())` 로 토큰을 주입하므로 이 결함을 **잡지 못한다**.
- 첫 write 가 401 이 아니라 **403** 이면 인증이 아니라 CSRF 문제 신호다.
