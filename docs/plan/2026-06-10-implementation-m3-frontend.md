# M3+M4: 프론트엔드 전체 구현 (React+Vite — 인증 + 연락처 관리)

> TDD 불가 영역(UI)은 순수 로직만 Vitest, 컴포넌트는 빌드+브라우저 검증. 진행상황은 하단 참조.

**Goal:** 엑셀 "수신처 관리"의 실제 사용자 화면. 로드맵 M3(부트스트랩+인증)와 M4(연락처 관리 화면)를 한 번에 구현. 디자인: 라이트 미니멀 실용.

**Stack:** React 18.3 + react-router-dom 6.28 + Vite 5.4 + Vitest 2.1, CSS Modules + 디자인 토큰, JS(TS 미사용). kanban-board 패턴 차용·대폭 단순화(SSE·세션타이머·테마·알림·프로젝트트리·로딩오버레이 전부 제외).

---

## 로드맵 위치
- M1 인증 ✅ / M2 연락처 API ✅ / **M3+M4(이 문서) 프론트 전체**
- 완료 시: `npm run dev`(proxy→8181) 또는 `npm run build`→Spring static 단일 산출물로 로그인·연락처 관리 동작.

## 핵심 설계 결정 (Plan 리뷰 + 통합검증 반영)
1. **Vite proxy**(`/api`→8181) same-origin → CORS 불필요, 쿠키 정상.
2. **apiFetch**: XSRF-TOKEN 쿠키→X-XSRF-TOKEN 헤더, credentials include, 401(비-auth)→`session-expired` 이벤트.
3. **DELETE는 repeated params** `?ids=1&ids=2`(설계 §6 comma 표기 수정).
4. **AuthContext 슬림**: me/login/register/logout/session-expired만. register는 세션 미생성 → 가입 후 로그인.
5. **useContacts**: 변이 후 refetch(목록+부서), contactsById Map 캐시(부서 가로지른 선택 이메일 해석), 부서 폴백, 삭제 시 해당 id만 선택/캐시 prune.
6. **선택 Set cross-department 유지**, 마스터 체크박스=visible 행만(union/difference), 검색 디바운스 300ms, 탭 전환 시 검색 초기화·선택 유지.
7. **provider**: ToastProvider>AuthProvider. 빈 선택 시 복사/삭제 비활성.
8. **백엔드 추가**: SpaWebConfig(딥링크 폴백), **CsrfCookieFilter**(아래 fix), 미인증 401(M2에서 추가).

## 통합 검증에서 발견·수정한 결함
- **SPA 첫 쓰기 403** (Spring Security 6 CSRF 지연 로딩 → login/me가 XSRF 쿠키 미발급). `CsrfCookieFilter` 추가로 해결. 상세: `docs/fix/2026-06-10-csrf-spa-first-write-403.md`.

## 주요 파일 (frontend/src/)
```
main.jsx(Provider) · App.jsx(라우팅·RequireAuth)
api/ client·auth·contacts    utils/ csrf·ids·selection·recipients
contexts/AuthContext  hooks/ useToast·useContacts
components/ common(Toast·ToastProvider·ConfirmDialog) · auth(AuthPage·LoginForm·RegisterForm)
           layout(AppLayout·Header·Sidebar) · dashboard(DashboardPage) · contacts(6개)
styles/ global.css(토큰) + *.module.css     test/ csrf·ids·selection·recipients
```
백엔드: `config/SpaWebConfig.java`, `config/CsrfCookieFilter.java`, `config/SecurityConfig.java`(수정)

---

## 진행 상황 (2026-06-10)
- [x] **Task 1**: Vite 부트스트랩 + SpaWebConfig + 디자인 토큰 — 커밋(빌드 성공, 컨텍스트 로드 PASS)
- [x] **Task 2**: apiFetch+CSRF · AuthContext · 로그인/회원가입 — Vitest 9
- [x] **Task 3**: AppLayout(Header+Sidebar) + 대시보드 placeholder
- [x] **Task 4**: 연락처 관리 화면(CRUD·부서탭·검색·다중선택·이메일복사·일괄삭제) — Vitest 25
- [x] **Task 5**: 통합 검증 + CSRF fix + 문서/메모리 + 커밋/푸시

**✅ M3+M4(프론트 전체) 완료.**

### 검증 증거
- 프론트 Vitest **25개 PASS**(selection 12·recipients 4·csrf 6·ids 3), `npm run build` 성공.
- 백엔드 **42개 PASS**(CsrfCookieFilter·SpaWebConfig 회귀 없음).
- **통합 스모크**(bootRun 8181 + curl): SPA index 200 · me 401 + XSRF 쿠키 발급 · register 201 · login 200 · **첫 연락처 추가 201**(403 결함 수정 확인) · departments · PUT · 일괄 DELETE.
- 의존성 취약점은 esbuild/vite dev 서버 한정(프로덕션 산출물 무관). vite 5 유지(검증 버전), 향후 메이저 업그레이드 시 함께.

### 수동 확인 권장(실사용 전)
`./gradlew bootRun`(터널) + `npm run dev` → `localhost:5173`에서 회원가입→로그인→연락처 CRUD/부서탭/검색/다중선택/이메일복사(클립보드)/일괄삭제. 클립보드는 https/localhost + 사용자 제스처 필요.

### 다음 (선택)
- Gradle Node 빌드 자동화(`npm run build`를 `./gradlew build`에 통합)
- 인증 보류 항목(me 401 HIGH-3·AuthService 분리 HIGH-4) — 백엔드 별도
- 프로덕션 배포(application-prod, HTTPS — 클립보드/Secure 쿠키)
