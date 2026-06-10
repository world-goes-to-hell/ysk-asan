# M6 — 비밀번호 변경 (본인)

> 작성 2026-06-10(수). 상태: **구현 완료 — 검증 통과**. 기능개선 백로그 1번(사용자 "한개씩 진행" 지시).

## 검증 결과
- 백엔드 테스트 66개 PASS(신규 5: 성공/현재비번 불일치 400/미인증 401/검증 400/**remember-me 쿠키 무효화**).
- 프론트 빌드 + Vitest 35개 PASS.
- 브라우저 E2E(localtest): 헤더 "비밀번호 변경" 버튼 → 다이얼로그 → 잘못된 현재 비번 "현재 비밀번호가
  올바르지 않습니다" 표시 → 정상 변경 토스트 + 세션 유지 → curl 로 옛 비번 401/새 비번 200 확인.

## 요구사항
- 로그인한 사용자가 **본인 비밀번호를 변경**할 수 있다(현재는 잊으면 DB 직접 리셋뿐).
- 현재 비밀번호 확인 후 변경(세션 탈취만으로 변경 불가).
- 헤더에서 진입(모달), 성공 토스트.

## 설계

### 백엔드
- `POST /api/auth/password` + `PasswordChangeRequest(currentPassword @NotBlank, newPassword @NotBlank @Size(8,72))` — register와 동일 정책.
- `SecurityConfig`: `.requestMatchers("/api/auth/password").authenticated()`를 `/api/auth/**` permitAll **앞에** 선언(더 구체적 규칙 우선). CSRF는 그대로 적용(ignore 목록은 register/login만 — 로그인 상태라 XSRF 쿠키 보유).
- `UserService.changePassword(username, current, new)`: `passwordEncoder.matches()`로 현재 비번 검증 → 불일치 시 `IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.")`(400) → 신규 비번 encode 후 `User.changePassword(encoded)` 도메인 메서드(dirty checking).
- 현재 비번 검증은 `AuthenticationManager`를 타지 않음(실패 카운트/잠금 미적용) — 세션 보유자의 current 비번 brute-force는 사내툴 리스크로 수용(주석 명시).
- **부수 보안 속성**: TokenBasedRememberMeServices 토큰 서명에 비밀번호가 포함 → **변경 즉시 기존 remember-me 쿠키 전부 자동 무효화**(탈취 토큰 차단). 세션은 유지(재로그인 불필요). 테스트로 증명.

### 프론트
- `authAPI.changePassword(current, next)`.
- `PasswordChangeDialog`(modal.module.css 재사용): 현재/새/새 확인 3필드, 클라이언트 검증(일치·8~72), 성공 토스트 "비밀번호가 변경되었습니다." + 서버 메시지 에러 표시.
- `Header`: 사용자명 옆 "비밀번호 변경" ghost 버튼 → 다이얼로그.

## 테스트 (AuthControllerTest 추가)
1. 성공: 변경 후 새 비번 로그인 200, 옛 비번 401
2. 현재 비번 불일치 → 400 + message
3. 미인증 → 401
4. 새 비번 8자 미만 → 400(검증)
5. **변경 시 기존 remember-me 쿠키 무효화**(쿠키로 me → 401)

## 검증
`./gradlew test`(터널 ON) + `npm --prefix frontend run build`/Vitest + bootRun(localtest) 브라우저 확인.

## 범위 밖
- 관리자의 타인 비밀번호 초기화(필요 시 후속), 비밀번호 복잡도 강화(Tier 보류 항목), 이메일 재설정(메일 인프라 없음).
