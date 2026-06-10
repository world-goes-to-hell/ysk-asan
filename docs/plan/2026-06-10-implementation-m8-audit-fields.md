# M8 — 연락처 변경 이력 (createdBy / updatedBy)

> 작성 2026-06-10(수). 상태: **구현 완료 — 검증 통과**. 기능개선 백로그 3번 — 전역 공유 데이터의 책임 추적 최소선.

## 검증 결과
- 백엔드 테스트 79개 PASS(신규 2: 생성 기록 / 타 사용자 수정 시 updatedBy 갱신·createdBy 유지).
- 발견·수정한 잠복 버그: 감사 리스너(@LastModifiedBy)와 @PreUpdate(updatedAt)는 flush 시점 실행이라
  update() 응답 DTO 에 옛 값이 실리던 문제 → 서비스에서 명시 flush 로 해결.
- 프론트 빌드 + Vitest 35개 PASS. 브라우저: "최근 수정" 컬럼에 `audadmin · 2026-06-10` 표시 확인.

## 요구사항
- 연락처를 **누가 등록했고 누가 마지막으로 수정했는지** 기록하고 화면에 표시.

## 설계

### 백엔드 — Spring Data JPA Auditing(표준 메커니즘)
- `JpaAuditingConfig`(신규): `@EnableJpaAuditing` + `AuditorAware<String>` 빈 —
  `SecurityContextHolder`에서 현재 username 추출(미인증/anonymous는 empty → null 저장).
- `Contact`: `@EntityListeners(AuditingEntityListener.class)` +
  `@CreatedBy String createdBy(updatable=false)` / `@LastModifiedBy String updatedBy`.
  - **nullable 컬럼**(NOT NULL 아님): 기존 행은 작성자를 알 수 없으므로 NULL이 정직한 값(grandfather).
    `ddl-auto=update`가 기존 테이블에 nullable 컬럼 추가는 무조건 안전.
  - 기존 수동 `@PrePersist/@PreUpdate`(createdAt/updatedAt)와 공존 — 날짜는 기존 방식 유지.
- `ContactResponse`에 `createdBy/updatedBy` 추가. CSV 컬럼은 변경하지 않음(가져오기 라운드트립 유지).
- User 엔티티에는 미적용(자가 가입이라 auditor 없음).

### 프론트
- 테이블에 "최근 수정" 컬럼: `updatedBy(없으면 -) · MM-DD`, hover tooltip(title)에 등록/수정 전체 정보.
- 편집 모드 행에 빈 셀 추가(컬럼 수 일치).

## 테스트 (ContactControllerTest)
1. 생성 시 createdBy/updatedBy = 현재 사용자(@WithMockUser(username=...))
2. 다른 사용자가 수정 → updatedBy 변경, createdBy 유지(updatable=false)

## 배포 주의
- 새 컬럼 2개(nullable) → 운영 배포 시 최초 1회 `DB_DDL_AUTO=update`(기존 절차).

## 범위 밖
- 변경 전/후 스냅샷 감사 로그(AuditLog 테이블), 삭제 이력(하드 삭제 유지) — 필요 시 후속.
