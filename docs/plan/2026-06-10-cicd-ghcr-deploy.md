# CI/CD: GHCR 이미지 자동 빌드 + 운영 배포

**Goal:** main 푸시 시 GitHub Actions 가 프론트+백엔드를 단일 Docker 이미지로 빌드해 GHCR 에 푸시하고, 개발서버(Dockge)에서 그 이미지로 배포한다.

## 구성 요소
| 파일 | 역할 |
|------|------|
| `Dockerfile` | 3-스테이지: ① node 프론트 빌드→static ② gradle bootJar(static 포함, `-x test`) ③ JRE 런타임(8181, Asia/Seoul) |
| `.dockerignore` | 빌드 컨텍스트 최소화 + 시크릿(application-local.yml·.env·db-tunnel) 제외 |
| `.github/workflows/ghcr-publish.yml` | main push/수동 → GHCR 빌드·푸시(`latest`+`sha`, amd64, gha 캐시, `GITHUB_TOKEN`) |
| `deploy/dokge/compose.prod.yaml` | 운영 스택: mariadb + app(`ghcr.io/world-goes-to-hell/ysk-asan:latest`), 내부망 `DB_HOST=mariadb` |
| `deploy/dokge/.env.example` | `WAS_PORT`, `DB_DDL_AUTO` 추가 |

## 이미지
- 이름: `ghcr.io/world-goes-to-hell/ysk-asan`
- 태그: `latest`(항상 최신), `<short-sha>`(커밋별 롤백용)
- 단일 실행물: 프론트 SPA 가 Spring static 에 포함(`jar tf` 로 `BOOT-INF/classes/static/index.html` 확인됨, ~52MB)

## 동작 흐름
```
git push main → Actions(ghcr-publish) → docker build(프론트+백엔드 단일 이미지) → GHCR push(latest, sha)
                                                                                      ↓
개발서버 Dockge: compose.prod.yaml → mariadb + app(GHCR pull) → 8181 서빙(SPA + API)
```

## 개발서버 배포 절차
1. `deploy/dokge/` 에서 `cp .env.example .env` → 실제 비밀번호/포트 입력.
2. **최초 1회**: `.env` 에 `DB_DDL_AUTO=update` 설정(빈 스키마에 `contact`/`users` 생성). 이후 배포부터 그 줄 삭제(validate).
3. **GHCR private 대응**(기본 private): 둘 중 하나
   - GitHub → Packages → `ysk-asan` 패키지 가시성을 **public** 으로, 또는
   - 개발서버에서 `echo <PAT> | docker login ghcr.io -u world-goes-to-hell --password-stdin` (PAT 스코프 `read:packages`).
4. `docker compose -f compose.prod.yaml up -d` (또는 Dockge 스택으로 등록). 갱신: `docker compose -f compose.prod.yaml pull && ... up -d`.
5. `http://<서버>:8181` 접속 → 회원가입/로그인/연락처.

## 사전 점검(저장소 설정)
- Settings → Actions → General → Workflow permissions: 기본 `GITHUB_TOKEN` 의 `packages: write`(워크플로 `permissions` 블록에 명시되어 별도 설정 불필요).

## 검증
1. **로컬(Docker 미설치)**: `npm --prefix frontend run build` + `./gradlew bootJar -x test` → `build/libs/ysk-asan-0.0.1-SNAPSHOT.jar` 생성, `jar tf` 로 static 포함 확인 ✅(Dockerfile 빌드 로직 모사).
2. **Actions**: main 푸시 → `gh run watch` 로 워크플로 GREEN → GitHub Packages 에 이미지 확인.

---

## 진행 상황 (2026-06-10)
- [x] Dockerfile + .dockerignore — 로컬 bootJar 단일 jar + static 포함 검증
- [x] .github/workflows/ghcr-publish.yml (+ docs/deploy 변경 시 빌드 스킵 paths-ignore)
- [x] compose.prod.yaml + .env.example + 본 문서
- [x] **푸시 → Actions 빌드 GREEN(2m40s) + GHCR push 성공** — run 27247254150, 모든 스텝 ✓

**✅ CI/CD 완료.** main 푸시 시 `ghcr.io/world-goes-to-hell/ysk-asan:latest`(+sha) 자동 생성.

### 알려진 경고 / 후속
- Actions 노드 deprecation: `actions/checkout@v4` 등이 Node 20 기반 — 2026-09 이후 제거 예정. 추후 액션 메이저 버전 업 필요(현재는 정상 동작).
- 첫 배포 전: GHCR 패키지 가시성(private 기본) → public 전환 또는 개발서버 `docker login ghcr.io`(PAT `read:packages`).

## 범위 밖(향후)
- 멀티아키(arm64), Flyway 마이그레이션, Actions→서버 SSH 자동 배포, 스테이징 환경, 이미지 취약점 스캔(Trivy)
