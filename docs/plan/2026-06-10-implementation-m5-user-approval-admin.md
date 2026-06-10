# M5 — 회원 승인제 + 권한/회원관리 + 로그인 실패 잠금

> 작성 2026-06-10(수). 상태: **구현 완료 — E2E 검증 통과**
> 사용자 결정: 기존 계정 전부 자동 승인(grandfather), 계획대로 진행.
> 커밋: a5d5aa1(M5-1·2 백엔드 코어) → 0fcd208(M5-3 관리자 API) → a21ead3(M5-4 프론트).

## 검증 결과 (2026-06-10)
- 백엔드 테스트 **60개 PASS** (신규 13: 승인/잠금 5 + 관리자 API 8), 프론트 빌드+Vitest 35개 PASS.
- **브라우저 E2E 전 시나리오 통과** (bootRun local,localtest + APP_BOOTSTRAP_ADMIN=ysk-admin):
  1. 가입 → "관리자 승인 후 로그인" 토스트 ✓
  2. 미승인 로그인 → "관리자 승인 대기 중인 계정입니다" 알림 ✓
  3. 재기동 → 부트스트랩 러너가 ysk-admin 승인+ADMIN 승격(로그) → 사이드바 "회원 관리" 노출 ✓
  4. member1 승인 버튼 → 정상 전환 + 토스트 → 로그인 가능 ✓
  5. 비밀번호 5회 오류: 1~4회 일반 메시지, 5회째 "5회 오류로 잠겼습니다", 이후 정답도 "계정이 잠겨 있습니다" ✓
  6. 회원 관리에 "잠김 · 실패 5회" 뱃지 → 잠금 해제 → 정답 로그인 200 ✓
  7. 권한 변경(ConfirmDialog) → 관리자 전환, 본인 행은 권한 버튼 미노출 ✓

## 요구사항 (사용자 확정)

1. **누구나 회원가입 가능** — 단, 가입 직후엔 `승인 대기` 상태.
2. **관리자 승인 후 로그인 가능** — 관리자 권한 회원이 "회원 관리" 페이지에서 승인.
3. **미승인 회원 로그인 시 알림** — "관리자 승인 대기 중" 메시지 표시(로그인 불가).
4. **비밀번호 5회 오류 → 계정 잠금** — 이후 올바른 비밀번호로도 로그인 불가.
5. **관리자가 회원 관리 페이지에서 잠금 해제** — 락 해제 버튼.
6. **권한관리** — 회원 관리 페이지에서 ADMIN/USER 권한 변경(권한관리 신설 요구 반영).

## 설계 결정 (검증된 Spring Security 통합 방식)

### 도메인 모델
`User` 엔티티에 4개 컬럼 추가 (Contact의 `update()` 도메인 메서드 패턴 미러링, @Setter 금지):

| 필드 | 타입/DDL | 의미 |
|---|---|---|
| `role` | `@Enumerated(STRING) UserRole` — `varchar(20) not null default 'USER'` | ADMIN / USER |
| `approved` | `boolean not null default false` | 관리자 승인 여부 |
| `locked` | `@Column(name="account_locked") boolean not null default false` | 5회 실패 잠금 |
| `failedLoginAttempts` | `int not null default 0` | 연속 실패 횟수 |

- columnDefinition에 **default를 명시** → `ddl-auto=update`가 기존 행이 있는 테이블에도 안전하게 ALTER(MariaDB 전용 프로젝트라 허용).
- 도메인 메서드: `approve()`, `unlock()`(locked=false+failed=0), `recordLoginFailure(max)`→잠금 여부 반환, `resetLoginFailures()`, `changeRole(role)`.
- 주의: `@Builder`는 미지정 필드가 null/0/false → `register()`에서 role=USER, approved=false 명시.

### 인증 흐름 (Spring Security 표준 메커니즘 활용)
- `CustomUserDetailsService`: UserDetails 빌더에 `.disabled(!approved)` + `.accountLocked(locked)` + `ROLE_{role}` 권한.
- **DaoAuthenticationProvider의 pre-auth 체크가 비밀번호 검사 전에 동작**: locked→`LockedException`, disabled→`DisabledException`. 순서: 잠김 > 승인대기 > 비밀번호오류.
  - 부수효과(올바름): 미승인/잠금 계정은 비밀번호 검사 전 차단 → 실패 카운트 미증가.
- `AuthController.login` catch 분기:
  - `LockedException` → 401 `{"message":"계정이 잠겨 있습니다. 관리자에게 잠금 해제를 요청하세요."}`
  - `DisabledException` → 401 `{"message":"관리자 승인 대기 중인 계정입니다. 승인 후 로그인할 수 있습니다."}`
  - `BadCredentialsException` → `userService.recordLoginFailure(username)`(존재하는 계정만 카운트, 미존재는 무시) → 방금 잠겼으면 잠금 메시지, 아니면 일반 메시지("사용자명 또는 비밀번호가 올바르지 않습니다.")
  - 로그인 성공 → `resetLoginFailures()` (카운터>0일 때만 write).
- `MAX_LOGIN_FAILURES = 5` 상수.
- **Remember-Me 상호작용**: `AbstractRememberMeServices`가 기본 `AccountStatusUserDetailsChecker`로 쿠키 소비 시 locked/disabled를 재검사 → 잠긴 계정의 기존 remember-me 쿠키는 자동 거부됨(테스트로 확인).
- 401 응답에 message 바디 추가 — 프론트 `apiFetch`가 `body.message`를 이미 추출하므로 계약 그대로.
  - 참고: 승인대기/잠금 메시지는 계정 존재를 노출하지만, 알림이 요구사항이므로 수용(사내툴).

### 관리자 API + 권한
- `SecurityConfig`: `.requestMatchers("/api/admin/**").hasRole("ADMIN")` (기존 `/api/**` authenticated보다 먼저).
- `AdminUserController` + `AdminUserService`(작은 파일 분리):
  - `GET  /api/admin/users` → `AdminUserResponse[]`(id, username, role, approved, locked, failedLoginAttempts, createdAt)
  - `POST /api/admin/users/{id}/approve`
  - `POST /api/admin/users/{id}/unlock`
  - `PATCH /api/admin/users/{id}/role` `{"role":"ADMIN"|"USER"}` — **자기 자신 권한 변경 금지**(관리자 0명 사태 방지)
- `UserNotFoundException` → 404 (ContactNotFoundException 패턴 미러링, GlobalExceptionHandler에 핸들러 추가).
- `UserResponse`에 `role` 추가(me/login 응답 — 프론트 메뉴 노출용).

### 최초 관리자 부트스트랩 (닭-달걀 해결 + break-glass)
- `AdminBootstrapRunner`(ApplicationRunner): `app.bootstrap-admin`(env `APP_BOOTSTRAP_ADMIN`, 기본 빈값=스킵)에 지정된 username을 **승인+ADMIN+잠금해제** 처리(멱등).
- 관리자 전원이 잠기는 사고의 break-glass 역할도 겸함(재기동 시 해당 계정 복구).
- compose.prod / .env.example에 `APP_BOOTSTRAP_ADMIN` 추가.

### 기존 계정 마이그레이션 (사용자 결정: 전부 자동 승인)
- **`approved` 컬럼의 DDL default를 `true`로** 설정 → `ddl-auto=update`의 ALTER가 기존 행을 전부 승인 상태로 backfill(grandfather).
- 신규 가입은 `register()`에서 **명시적으로 `approved=false`** — JPA INSERT는 항상 명시값을 쓰므로 DDL default와 무관.
- 러너/플래그 방식 대비 장점: 재기동 시 승인대기 회원이 잘못 자동 승인되는 race 자체가 없음(default는 ALTER backfill에만 작용).
- `role`의 default는 `'USER'` → 기존 계정은 전부 USER로 backfill, 본인 계정의 ADMIN 승격은 `APP_BOOTSTRAP_ADMIN`이 담당.

### 프론트
- `api/admin.js`(list/approve/unlock/changeRole), `AuthContext`는 무변경(me 응답에 role 포함되므로 자동).
- `App.jsx`: `RequireAdmin`(role!=='ADMIN' → `/`로 리다이렉트) + `/admin/users` 라우트.
- `Sidebar`: "회원 관리" 메뉴 — ADMIN에게만 노출.
- `AdminUsersPage`: 테이블(사용자명/권한/상태뱃지[승인대기·정상·잠김]/가입일) + 승인·잠금해제·권한변경 버튼, 기존 ConfirmDialog/Toast 재사용, contacts 테이블 스타일 미러링.
- `LoginForm`: 401 고정 문구 대신 **서버 message 우선 표시**(승인대기/잠김 구분 알림).
- `RegisterForm`: 가입 성공 토스트 → "가입 완료 — 관리자 승인 후 로그인할 수 있습니다."

## 기존 테스트 영향 (중요)
- register→login 패턴의 기존 테스트(carol/dave/erin/rm* 등)는 **미승인 401로 깨짐** → `registerApproved(username)` 헬퍼(가입 후 UserRepository로 승인) 도입해 일괄 수정. ContactControllerTest의 로그인 헬퍼도 동일 점검.

## Tasks

1. **M5-1 도메인+부트스트랩**: UserRole, User 필드+도메인 메서드, UserResponse(role)/AdminUserResponse, UserNotFoundException+핸들러, AdminBootstrapRunner, yml/compose. `compileJava`.
2. **M5-2 인증 흐름(TDD)**: CustomUserDetailsService 플래그, UserService(record/reset), AuthController 분기. 신규 테스트: 미승인 401 메시지 / 승인 후 200 / 5회 실패→정답도 401 잠금 / 4회 실패 후 성공→카운터 리셋 / 잠금 후 remember-me 쿠키 거부. 기존 테스트 registerApproved 전환.
3. **M5-3 관리자 API(TDD)**: AdminUserService/Controller, SecurityConfig admin 규칙. 테스트: 비관리자 403 / 미인증 401 / 목록 / 승인→로그인 가능 / 해제→로그인 가능 / 자기권한변경 400 / 없는 id 404.
4. **M5-4 프론트**: admin api, AdminUsersPage(+스타일), Sidebar·RequireAdmin·라우트, LoginForm/RegisterForm 메시지. 빌드+Vitest.
5. **M5-5 검증/배포/문서**: bootRun(localtest) 브라우저 E2E(가입→미승인 알림→관리자 승인→로그인→5회 실패 잠금→해제), compose/.env.example, 문서/메모리, 커밋·푸시.

## 검증
```bash
./gradlew test                                   # 터널 ON, ysk_asan_test 격리
npm --prefix frontend run build && npm --prefix frontend test
./gradlew bootRun --args='--spring.profiles.active=local,localtest'   # 수동 E2E (+APP_BOOTSTRAP_ADMIN)
```

## 리스크

| 리스크 | 가능성 | 대응 |
|---|---|---|
| 기존 register→login 테스트 대량 파손 | 확실 | registerApproved 헬퍼로 일괄 전환(M5-2에 포함) |
| 운영 기존 계정 전원 승인대기(본인 포함) | 확실 | APP_BOOTSTRAP_ADMIN 필수 안내(compose 주석+문서) |
| prod 스키마(validate)에 새 컬럼 없음 | 확실 | 배포 시 1회 DB_DDL_AUTO=update(기존 절차와 동일) |
| 관리자 전원 셀프 잠금 | 낮음 | 부트스트랩 러너가 break-glass + 자기권한변경 금지 |
| 잠긴 계정의 기존 remember-me 쿠키 | 낮음 | AccountStatusUserDetailsChecker 기본 거부 + 테스트 |

## 범위 밖
- 가입 거부/계정 삭제, 비밀번호 변경(별도 Tier 2 항목), 이메일 알림, 잠금 자동 해제(시간 경과), Flyway(Tier 3).
