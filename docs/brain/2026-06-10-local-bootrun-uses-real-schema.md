# 로컬 bootRun 은 ysk_asan_test 가 아니라 실 공유 스키마(ysk_asan)를 쓴다

- 날짜: 2026-06-10
- 분류: 환경 함정(반복 실수) — 수동 검증이 실 DB 를 오염

## 무엇이 헷갈렸나
"테스트는 ysk_asan_test 를 쓴다"는 사실을 **수동 실행에도 적용된다고 착각**했다. 실제로는:

| 실행 방식 | 스키마 | 근거 |
|-----------|--------|------|
| `./gradlew test`(자동 통합테스트) | **ysk_asan_test** (create-drop) | `IntegrationTest` 의 `@DynamicPropertySource` 가 URL만 override |
| `./gradlew bootRun`(local profile) | **ysk_asan** (실 공유, ddl-auto=update) | `application-local.yml` 의 `url: .../ysk_asan` |

즉 `ysk_asan_test` 격리는 **자동 테스트 한정**이고, 손으로 띄우는 bootRun 은 운영/개발이 공유하는 실 스키마로 바로 들어간다.

## 결과
M3 통합 검증(curl)과 디자인 검증(브라우저)에서 bootRun 을 그대로 써서, `ysk_asan.users` 에 더미 계정 5개(`smoke×3`, `cleanup×1`, `asanadmin`)가 쌓였다. 연락처는 삭제했지만 사용자 삭제 API 가 없어 계정은 잔존(사용자 결정으로 보존).

## 재발 방지
수동 실행/검증은 **`local,localtest` 프로파일을 함께** 활성화해 ysk_asan_test 로 격리한다:
```
./gradlew bootRun --args='--spring.profiles.active=local,localtest'
```
`application-localtest.yml` 이 url 만 ysk_asan_test 로 덮고 ddl-auto=update(계정/비번은 local 재사용). 시크릿 없어 커밋됨.

## 교훈
- "테스트 스키마를 쓴다"는 말은 **어떤 실행 경로**인지 항상 따져야 한다. `@DynamicPropertySource`/`@TestPropertySource` override 는 그 테스트 컨텍스트에서만 적용된다.
- 공유 DB 환경에서 손으로 앱을 띄울 땐 **무조건 격리 스키마**를 먼저 확인하고 실행한다.
