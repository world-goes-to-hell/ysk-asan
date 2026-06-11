# M10 — 공문 관리 (양식 갤러리 + 직인 URL 발급)

> 작성 2026-06-10(수). 상태: **설계(사용자 확인 대기)**

## 요구 흐름 (사용자 정의 8단계)

1. **공문 관리 메뉴** → 공문 양식 썸네일 **갤러리**
2. 양식 클릭 →
3. **양식 미리보기 + 입력 폼**(양식마다 입력 항목이 다름 → 프론트에 하드코딩)
4. **직인 URL 발급** 버튼 → 비밀번호 입력(발급자가 설정)
5. 작성된 공문을 볼 수 있는 **URL 발급**
6. URL 접근(로그인 불필요) → 비밀번호 입력(4차에서 설정한 것)
7. 공문 표시 + **직인 삽입** 버튼 → 직인 이미지 업로드 창
8. 업로드 → 공문의 **(인) 영역에 직인 이미지 삽입**

핵심 사용 시나리오: 직원이 공문을 작성해 URL+비밀번호를 직인 보유자(예: 행정실)에게 전달
→ 보유자가 로그인 없이 열람·직인 날인.

## 설계

### 도메인 (백엔드)
새 엔티티 `OfficialDocument` (발급본):

| 필드 | 타입 | 설명 |
|---|---|---|
| shareToken | varchar(64) unique | URL 토큰 — `SecureRandom` 43자(256bit base64url) |
| templateId | varchar(50) | 양식 식별자(백엔드 화이트리스트로 검증) |
| fieldsJson | TEXT | 입력값 Map → JSON(총 10KB 한도) |
| passwordHash | varchar | **bcrypt**(평문 저장 금지) |
| sealImage / sealContentType | MEDIUMBLOB / varchar | 직인 이미지(png/jpeg, 1MB 한도). DB 저장 — 파일시스템/볼륨 불필요 |
| failedAttempts | int | 비밀번호 오류 카운트 — **10회 잠금**(공개 엔드포인트 brute-force 방어, 잠기면 재발급) |
| createdBy / createdAt | | 발급자 추적(JPA Auditing 재사용) |

### API
| 엔드포인트 | 인증 | 동작 |
|---|---|---|
| `POST /api/documents` | **로그인 필수** | {templateId, fields, password} → {token, url} (4·5차) |
| `POST /api/documents/{token}/view` | 공개 | {password} → {templateId, fields, sealImageBase64?} — 매 요청 비번 검증(무상태) (6·7차) |
| `POST /api/documents/{token}/seal` | 공개 | multipart(password + image) → 직인 저장 (8차) |

- 공개 2개만 `permitAll`(기존 `/api/**` authenticated 앞에 선언), **CSRF는 그대로 적용**
  (익명에게도 CsrfCookieFilter가 XSRF 쿠키 발급 — 기존 인프라 재사용).
- 비밀번호 검증 후 상태를 만들지 않음(무상태) — 뷰어가 비번을 메모리에 들고 view/seal에 동봉.
- 직인 이미지는 view 응답에 base64 포함(별도 이미지 URL에 비번 노출 방지).
- 토큰 미존재/잠김은 동일 404(존재 비노출), 비번 오류는 401 + 남은 시도 비노출.

### 양식(템플릿) — 프론트 하드코딩
- `frontend/src/documents/templates.jsx` 레지스트리: `{id, title, summary, fields[], DocumentView}`
  - `fields[]`: `{key, label, multiline?, placeholder}` — 입력 폼 자동 생성
  - `DocumentView`: 양식별 HTML/CSS 공문 레이아웃(A4 비율 용지, (인) 영역 포함) — 발급자
    미리보기와 공개 뷰어가 **같은 컴포넌트** 사용(렌더 일치 보장)
- **썸네일 = 실제 양식의 축소 렌더**(CSS scale) — HWP 스크린샷 이미지 파일 불필요, 양식 수정 시 썸네일 자동 일치
- 초기 2종(실제 원내 HWP 양식 확보 전 일반 양식으로 구성, 추후 교체 용이):
  1. **일반 공문**: 문서번호/시행일자/수신/참조/제목/본문/발신명의 + (인)
  2. **재직증명서**: 성명/부서/직위/재직기간/용도/발급일 + 기관장 (인)

### 프론트 화면
- 사이드바 "공문 관리"(전 사용자) → `/documents` 갤러리(카드: 축소 미리보기+제목)
- `/documents/:templateId` 작성 화면: 좌 입력 폼 / 우 실시간 미리보기 → "직인 URL 발급"
  → 비밀번호 모달(입력+확인) → 발급 완료 모달(URL + 복사 버튼)
- `/d/:token` **공개 뷰어**(RequireAuth 밖, SPA 폴백으로 동작 확인됨): 비밀번호 게이트 →
  공문 렌더 → (인) 영역: 직인 없으면 "직인 삽입" 버튼(file input) → 업로드 → (인) 위에 직인 합성

### 직인 합성 방식
- DocumentView의 (인) 표기 위에 `position:absolute` 이미지 오버레이(70~90px, 반투명 — 실제
  날인 느낌). 인쇄(Ctrl+P) 시에도 유지되도록 print CSS 포함.

## 테스트 (OfficialDocumentControllerTest 신규)
1. 발급: 미인증 401 / 정상 201 + 43자 토큰 / 템플릿 화이트리스트 외 400
2. 열람: 비번 오류 401 / 정상 200(fields 복원) / 토큰 미존재 404 / **10회 오류 → 잠김(404)**
3. 직인: 비번 검증 / png 업로드 후 view에 base64 포함 / 1MB 초과·비이미지 400
4. CSRF: 공개 POST도 토큰 없으면 403

## 작업 분할
1. **M10-1 백엔드**(엔티티/서비스/컨트롤러/시큐리티 + 테스트)
2. **M10-2 템플릿+갤러리**(레지스트리, 2종 양식 렌더, 갤러리·작성 화면)
3. **M10-3 발급·공개 뷰어**(비밀번호 모달→URL 발급, /d/:token 게이트→직인 업로드·합성)
4. **M10-4 E2E**(8단계 전체 흐름 브라우저 검증) + 보안 리뷰 + 문서/커밋/푸시

## 배포 주의
- 새 테이블 1개 → 운영 최초 1회 `DB_DDL_AUTO=update`(기존 절차).

## 범위 밖(후속 후보)
- 발급한 URL 목록/회수(만료·삭제) 관리, URL 유효기간, HWP 파일 자체 렌더링/다운로드,
  직인 위치 드래그 조정, 발급 알림.
