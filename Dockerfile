# ── Stage 1: Frontend Build ──────────────────────────
# React(Vite) 빌드 산출물을 src/main/resources/static 으로 출력(vite.config outDir).
FROM node:20-alpine AS frontend

WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build
# 결과: /app/src/main/resources/static/ (outDir = ../src/main/resources/static)

# ── Stage 2: Backend Build ───────────────────────────
# 프론트 산출물을 포함해 단일 실행 jar(bootJar)를 만든다.
# 통합 테스트는 SSH 터널 DB 에 의존하므로 이미지 빌드 시에는 스킵한다(-x test).
FROM gradle:8.6-jdk17 AS build

WORKDIR /app
COPY build.gradle settings.gradle ./
COPY gradle/ gradle/
COPY src/ src/
COPY --from=frontend /app/src/main/resources/static/ src/main/resources/static/
RUN gradle bootJar -x test --no-daemon

# ── Stage 3: Runtime ─────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
RUN apk add --no-cache tzdata \
    && cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime \
    && echo "Asia/Seoul" > /etc/timezone

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8181

ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-jar", "app.jar"]
