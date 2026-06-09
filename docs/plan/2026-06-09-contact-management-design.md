# 수신처 관리 시스템 — 설계 문서

- 작성일: 2026-06-09
- 상태: 설계 확정 (구현 계획 수립 대기)
- 참고 프로젝트: `D:\DEV\WORKSPACE\PERSONAL\kanban-board` (스택·패턴 차용)
- GitHub: https://github.com/world-goes-to-hell/ysk-asan.git
- 개발/운영 DB: **MariaDB** (Dockge 스택), 개발 시 로컬에서도 동일 DB 사용

## 1. 배경 / 목적

기존 엑셀 기반 "수신처 관리" 시트를 웹 기반 시스템으로 전환한다.
엑셀의 핵심 가치는 **연락처 등록 → 부서 필터 → 다중 선택 → 이메일 일괄 복사**(메일 수신처 작성용)이다.

이번 버전은 **로그인 + 대시보드(placeholder) + 연락처 관리** 까지만 구현한다.
사이드 메뉴는 향후 다른 "관리" 메뉴가 추가될 수 있도록 확장 가능한 구조로 만든다.

## 2. 확정된 요구사항

| 항목 | 결정 |
|------|------|
| 스택 | Spring Boot 3.2 (Java 17) + React 18(Vite) — 새 프로젝트로 스캐폴딩 |
| 인증 | 회원가입 + 로그인 (세션 기반, 다중 사용자) |
| 연락처 데이터 | **전역 공유** (모든 로그인 사용자가 동일 목록을 공유·편집) |
| 부서 | 자유 입력 → 입력된 부서들로 부서 탭 자동 생성 |
| 연락처 필드 | 부서 / 이름 / 이메일 |
| 메뉴 | 대시보드(placeholder) + 연락처 관리 |
| 향후 메뉴 | 지금은 숨김 (대시보드 + 연락처 관리만 노출) |
| 검색 | 부서 필터 + 이름·이메일 텍스트 검색 |
| CSV import | 이번 버전 제외 (향후) |

## 3. 보안 방침 (표준 보호)

| 대상 | 방식 | 구현 위치 |
|------|------|-----------|
| 비밀번호 | **BCrypt 단방향 해싱** (복호화 불가) | 애플리케이션 코드 |
| 전송 구간 | **HTTPS/TLS** | 운영 배포(리버스 프록시/LB) |
| 저장(at-rest) | **DB/디스크 레벨 암호화** | 운영 인프라 설정(MariaDB/디스크 암호화) |
| 접근통제 | 전 `/api/**` 인증 필수 + CSRF 쿠키 토큰 | 애플리케이션 코드 |
| 입력 검증 | 이메일 형식·필수값 (Bean Validation) | 애플리케이션 코드 |
| 시크릿 | DB 자격증명 등 **환경변수**로 관리 | 배포 설정 |

### 중요 메모
- 비밀번호와 연락처는 보호 방식이 **다르다**.
  - 비밀번호: 단방향 **해싱**(분쇄기 — 복구 불가, 대조만 가능).
  - 연락처(이메일·이름): 화면 표시·복사가 필요하므로 평문으로 다루며, 저장 보호는 **인프라 레벨 at-rest 암호화**로 처리한다.
- at-rest 암호화는 **코드가 아니라 운영 인프라에서 켜는 설정**이다.
  개발 환경(H2 인메모리)에서는 해당 없음. 운영 MariaDB 배포 시 적용.
- 연락처 필드 자체를 암호문으로 저장하는 컬럼 레벨 암호화(AES)는 검색/부서탭과
  충돌(blind index 필요)하므로 이번 버전에서는 채택하지 않는다. (향후 보안 강화 옵션)

## 4. 아키텍처

```
ysk-asan/
├── build.gradle                         # Spring Boot, JPA, Security, Validation, MariaDB 드라이버, Lombok
├── src/main/java/com/ysk/contact/
│   ├── ContactApplication.java
│   ├── config/
│   │   ├── SecurityConfig.java          # 인증/CSRF/권한 매처
│   │   ├── PasswordEncoderConfig.java   # BCrypt
│   │   ├── SpaWebConfig.java            # React 빌드 SPA 라우팅 포워딩
│   │   ├── GlobalExceptionHandler.java  # 통일된 에러 응답
│   │   └── DataInitializer.java         # (선택) 초기 admin/샘플
│   ├── controller/  AuthController, ContactController
│   ├── service/     UserService, ContactService
│   ├── repository/  UserRepository, ContactRepository
│   ├── entity/      User, Contact
│   └── dto/         RegisterRequest, LoginRequest, ContactRequest, ContactResponse, ...
├── src/main/resources/
│   ├── application.yml                   # 공통
│   ├── application-dev.yml               # 로컬 MariaDB(Dockge) 접속
│   └── application-prod.yml              # MariaDB(환경변수)
└── frontend/                             # React + Vite
    └── src/
        ├── api/        client.js(apiFetch+CSRF), auth.js, contacts.js
        ├── contexts/   AuthContext.jsx
        ├── components/
        │   ├── auth/       AuthPage, LoginForm, RegisterForm
        │   ├── layout/     AppLayout, Sidebar, Header
        │   ├── dashboard/  DashboardPage (placeholder 이미지)
        │   ├── contacts/   ContactsPage, ContactAddForm, DepartmentTabs,
        │   │               ContactTable, ContactRow, SelectionBar
        │   └── common/     Modal, ConfirmDialog, Toast
        └── styles/     *.module.css
```

- 개발: **로컬 MariaDB**(Dockge 스택 `deploy/dokge/compose.yaml`) + Vite dev 서버(API 프록시). `docker compose up -d` 로 DB 기동 후 `./gradlew bootRun` + `npm run dev`.
- 운영: MariaDB(Dockge). `npm run build` 산출물을 Spring Boot static으로 서빙 → 단일 실행물.
- 테스트: 단위는 mock, 통합은 동일 MariaDB(또는 Testcontainers). 개발/운영과 같은 DB로 방언 차이를 예방.

## 5. 데이터 모델

```
User                              Contact  (전역 공유, 소유자 구분 없음)
──────────────                    ──────────────────────────────
id (PK, auto)                     id (PK, auto)
username (unique, not null)       department  (not null)   # 부서, 자유 입력
password (BCrypt, not null)       name        (not null)   # 이름
createdAt                         email       (not null)   # 이메일
                                  createdAt
                                  updatedAt
```

- 연락처는 전역 공유이므로 `ownerId` 없음.
- 부서 탭 목록은 `SELECT DISTINCT department FROM contact ORDER BY department` 로 동적 생성.
- 이메일 중복은 **허용**(한 사람이 여러 부서 소속 가능). 단, 추가 시 동일 이메일이
  이미 있으면 프론트에서 **경고만** 표시(차단하지 않음).

## 6. 백엔드 API

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/api/auth/register` | 회원가입(username, password) | 공개 |
| POST | `/api/auth/login` | 로그인(세션 발급) | 공개 |
| POST | `/api/auth/logout` | 로그아웃 | 필요 |
| GET | `/api/auth/me` | 현재 로그인 사용자 | 필요 |
| GET | `/api/contacts?department=&q=` | 목록(부서·검색 필터) | 필요 |
| GET | `/api/contacts/departments` | 부서 탭 목록(distinct) | 필요 |
| POST | `/api/contacts` | 연락처 추가 | 필요 |
| PUT | `/api/contacts/{id}` | 연락처 수정(오타 보정) | 필요 |
| DELETE | `/api/contacts?ids=1,2,3` | 선택 항목 일괄 삭제 | 필요 |

- 응답 형식: 일관된 envelope 또는 단순 DTO. (kanban-board 패턴에 맞춰 단순 DTO 채택)
- 에러: `GlobalExceptionHandler`에서 `{ message }` 형태로 통일, 검증 실패는 400.
- 보안: `/api/auth/**` permitAll, 그 외 `/api/**` authenticated. CSRF는 쿠키 토큰
  (`X-XSRF-TOKEN`), 비변경 메서드(GET) 제외.

## 7. 연락처 관리 화면 (엑셀 → 웹 매핑)

| 엑셀 기능 | 웹 구현 |
|-----------|---------|
| ① 연락처 추가 (부서/이름/이메일 + 추가) | 상단 인라인 추가 폼(`ContactAddForm`) |
| ② 부서 탭 (전체/부서별) | `DepartmentTabs` — distinct 부서로 자동 생성, "전체" 포함 |
| ③ 명단 + 체크박스 | `ContactTable` — 행 체크박스, 이름·이메일 검색창, 행별 수정/삭제 |
| ④ 선택 인원 / 수신자 결과 | `SelectionBar` — 선택 카운트 배지 |
| 전체선택 / 전체해제 | 테이블 헤더 마스터 체크박스 |
| 이메일 복사 | `navigator.clipboard.writeText` — 선택 이메일 `;` 구분 일괄 복사 |
| 삭제 | 선택 항목 일괄 삭제(`ConfirmDialog` 확인 후 DELETE) |

- 선택 상태는 프론트 로컬 state(`Set<id>`)로 관리(immutable 갱신).
- 부서 필터/검색은 쿼리 파라미터로 백엔드 위임(데이터 증가 대비).

## 8. 프론트엔드 라우팅 / 레이아웃

```
/login                → AuthPage (로그인/회원가입 탭)
/ (RequireAuth)
  └ AppLayout (Sidebar + Header + Outlet)
       ├ index        → DashboardPage (placeholder)
       └ /contacts    → ContactsPage
```

- `RequireAuth`로 미인증 시 `/login` 리다이렉트(kanban-board 패턴).
- `Sidebar`: "대시보드", "연락처 관리" 메뉴. 향후 메뉴는 추가 지점만 마련(현재 숨김).
- `apiFetch`: `credentials: 'include'` + CSRF 토큰 자동 첨부, 401 시 세션 만료 처리.

## 9. 이번 버전 범위 / 향후 확장

### 포함 (이번 버전)
- 회원가입 / 로그인 / 로그아웃 / 현재 사용자 조회
- 대시보드(placeholder 이미지)
- 연락처 CRUD(추가/수정/삭제) + 부서 필터 + 이름·이메일 검색
- 다중 선택 → 이메일 일괄 복사 / 일괄 삭제 / 전체선택·해제

### 제외 (향후)
- 추가 "관리" 메뉴 2개
- CSV/엑셀 import·export
- 권한 분리(admin 전용 기능)
- 연락처 그룹/태그, 컬럼 레벨 암호화(보안 강화)

## 10. 테스트 전략

- 백엔드
  - `ContactService` 단위 테스트: 추가/수정/삭제, 부서 distinct, 검색 필터
  - `ContactController` 통합 테스트(MockMvc): 인증 가드, 검증 실패(400), CRUD
  - `AuthController` 통합 테스트: 회원가입/로그인/세션
  - 목표 커버리지 80%+
- 프론트엔드
  - 선택 관리(Set immutable), 이메일 복사 문자열 생성 등 순수 로직 위주

## 11. 리스크 / 메모

- Windows 환경 + Gradle/Node 빌드 — 스캐폴딩 시 경로/줄바꿈(CRLF) 주의.
- React 빌드 산출물을 Spring static으로 서빙하는 SPA 포워딩 설정 필요(SpaWebConfig).
- 새 프로젝트이므로 git 저장소 초기화는 구현 계획 1단계에서 진행.

## 12. 형상관리 / 인프라

### Git
- 원격: https://github.com/world-goes-to-hell/ysk-asan.git
- 개인정보·시크릿 보호: `.gitignore` 로 `.env`, 키/인증서, `*.xlsx/*.csv/*.hwp` 등
  개인정보가 담길 수 있는 파일을 추적 제외. 시크릿은 `.env`(미추적)로만 관리하고
  저장소에는 `.env.example`(예시 값)만 커밋한다.

### 개발 DB (Dockge + MariaDB)
- 스택 파일: `deploy/dokge/compose.yaml` (MariaDB 11.4 단일 서비스)
- 환경 변수: `deploy/dokge/.env`(미추적), 템플릿은 `deploy/dokge/.env.example`
- 기동: `cd deploy/dokge && cp .env.example .env && docker compose up -d`
- 로컬 접속: `jdbc:mariadb://localhost:3306/ysk_asan`
- 개발/운영 모두 동일 MariaDB 사용 → H2 방언 차이로 인한 버그 예방
- 백엔드 의존성: `org.mariadb.jdbc:mariadb-java-client`

### 포트 사용 계획
- MariaDB: `3306`
- WAS(Spring Boot 애플리케이션): **`8181`** (`server.port=8181`)
- Vite 개발 서버: `5173` → `/api` 요청을 `8181` 로 프록시
