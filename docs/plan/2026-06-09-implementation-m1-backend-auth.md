# M1: 백엔드 부트스트랩 + 인증 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Spring Boot 백엔드를 부트스트랩하고 MariaDB에 연결한 뒤, 회원가입·로그인·로그아웃·현재 사용자 조회(세션 기반 인증)를 동작시킨다.

**Architecture:** Spring Boot 3.2 레이어드(controller→service→repository→entity). Spring Security 세션 기반 + CSRF 쿠키 토큰, 비밀번호 BCrypt. 통합 테스트는 Testcontainers MariaDB로 운영과 동일한 DB에서 검증한다.

**Tech Stack:** Java 17, Spring Boot 3.2.3 (web/data-jpa/security/validation), MariaDB 11.4, Lombok, JUnit5 + MockMvc + Testcontainers.

---

## 전체 로드맵에서의 위치

- **M1 (이 문서)**: 백엔드 부트스트랩 + DB 연결 + 인증
- M2: 연락처 도메인 API (Contact CRUD + 부서/검색 + 일괄삭제)
- M3: 프론트 부트스트랩 (Vite + 라우팅 + AuthContext + 레이아웃/사이드바 + 로그인 화면)
- M4: 연락처 관리 화면 + 대시보드(placeholder)

**M1 완료 시 검증 가능한 결과물:** `./gradlew bootRun`(local profile, SSH 터널 필요) 으로 8181 기동 → `users` 테이블 자동 생성 → 회원가입/로그인/로그아웃/me API 동작. Testcontainers 통합 테스트 전부 GREEN.

---

## 사전 조건

- DB 연결 검증 완료(`scripts/db_test.py` 성공). 로컬 실행 시 `scripts/db-tunnel.ps1` 터널 필요.
- 설정 파일 존재: `src/main/resources/application.yml`, `application-local.yml`(실제), `application-prod.yml`.
- **Docker 필요**(Testcontainers 통합 테스트용). 미설치 시 Task 1 Step 7 참고.
- Gradle wrapper 생성에 로컬 `gradle` 또는 IntelliJ 필요(Task 1 참고).

## 파일 구조 (M1에서 생성)

```
build.gradle                 # 의존성/플러그인
settings.gradle              # rootProject.name
gradlew / gradlew.bat        # wrapper (gradle wrapper 로 생성)
gradle/wrapper/*             # wrapper 메타

src/main/java/com/ysk/contact/
├── ContactApplication.java                 # @SpringBootApplication
├── entity/User.java                        # 사용자 엔티티(users)
├── repository/UserRepository.java          # findByUsername/existsByUsername
├── dto/RegisterRequest.java                # 회원가입 요청(record + validation)
├── dto/LoginRequest.java                   # 로그인 요청
├── dto/UserResponse.java                   # 사용자 응답(비번 제외)
├── service/UserService.java                # 회원가입/조회 비즈니스 로직
├── service/CustomUserDetailsService.java   # Spring Security UserDetailsService
├── controller/AuthController.java          # /api/auth/*
└── config/
    ├── PasswordEncoderConfig.java          # BCrypt 빈
    ├── SecurityConfig.java                 # 보안 필터체인/CSRF
    └── GlobalExceptionHandler.java         # 검증/예외 → 일관 JSON

src/test/java/com/ysk/contact/
├── support/IntegrationTest.java            # Testcontainers 베이스 클래스
├── ContactApplicationTests.java            # 컨텍스트 로드
├── repository/UserRepositoryTest.java      # 리포지토리 슬라이스
└── controller/AuthControllerTest.java      # 인증 API 통합

src/test/resources/
└── application-test.yml                    # 테스트 공통(선택)
```

---

## Task 1: Gradle / Spring Boot 부트스트랩

**Files:**
- Create: `settings.gradle`, `build.gradle`
- Create: `src/main/java/com/ysk/contact/ContactApplication.java`
- Create: `src/test/java/com/ysk/contact/support/IntegrationTest.java`
- Test: `src/test/java/com/ysk/contact/ContactApplicationTests.java`

- [ ] **Step 1: settings.gradle 작성**

`settings.gradle`:
```groovy
rootProject.name = 'ysk-asan'
```

- [ ] **Step 2: build.gradle 작성**

`build.gradle`:
```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.3'
    id 'io.spring.dependency-management' version '1.1.4'
}

group = 'com.ysk'
version = '0.0.1-SNAPSHOT'

java {
    sourceCompatibility = '17'
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    runtimeOnly 'org.mariadb.jdbc:mariadb-java-client'
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
    testImplementation 'org.springframework.boot:spring-boot-testcontainers'
    testImplementation 'org.testcontainers:junit-jupiter'
    testImplementation 'org.testcontainers:mariadb'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

- [ ] **Step 3: 메인 클래스 작성**

`src/main/java/com/ysk/contact/ContactApplication.java`:
```java
package com.ysk.contact;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ContactApplication {
    public static void main(String[] args) {
        SpringApplication.run(ContactApplication.class, args);
    }
}
```

- [ ] **Step 4: Testcontainers 베이스 클래스 작성**

`src/test/java/com/ysk/contact/support/IntegrationTest.java`:
```java
package com.ysk.contact.support;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public abstract class IntegrationTest {

    @Container
    static MariaDBContainer<?> mariadb =
            new MariaDBContainer<>("mariadb:11.4")
                    .withDatabaseName("ysk_asan_test");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mariadb::getJdbcUrl);
        registry.add("spring.datasource.username", mariadb::getUsername);
        registry.add("spring.datasource.password", mariadb::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "false");
    }
}
```

- [ ] **Step 5: 컨텍스트 로드 테스트 작성 (failing)**

`src/test/java/com/ysk/contact/ContactApplicationTests.java`:
```java
package com.ysk.contact;

import org.junit.jupiter.api.Test;
import com.ysk.contact.support.IntegrationTest;

class ContactApplicationTests extends IntegrationTest {

    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 6: Gradle wrapper 생성 후 테스트 실행**

먼저 wrapper 생성(로컬 gradle 필요. 없으면 IntelliJ에서 프로젝트 import 시 자동 생성):
```bash
gradle wrapper --gradle-version 8.5
```
그 다음 테스트:
```bash
./gradlew test --tests "com.ysk.contact.ContactApplicationTests"
```
Expected: **PASS** (Testcontainers가 mariadb:11.4 컨테이너를 띄우고 컨텍스트 로드 성공)

- [ ] **Step 7: (Docker 없을 경우) 임시 대안 확인**

Docker 미설치면 위 테스트는 컨테이너 기동 실패로 멈춘다. 그 경우 Docker Desktop 설치가 정석. 임시로 빠른 확인만 원하면 `./gradlew build -x test` 로 컴파일만 검증하고, 통합 테스트는 Docker 준비 후 수행한다. (이 사실을 PR/커밋 메시지에 명시)

- [ ] **Step 8: Commit**

```bash
git add settings.gradle build.gradle gradlew gradlew.bat gradle/ src/main/java src/test/java
git commit -m "feat(backend): Spring Boot 부트스트랩 + Testcontainers 컨텍스트 로드 테스트"
```

---

## Task 2: User 엔티티 + UserRepository

**Files:**
- Create: `src/main/java/com/ysk/contact/entity/User.java`
- Create: `src/main/java/com/ysk/contact/repository/UserRepository.java`
- Test: `src/test/java/com/ysk/contact/repository/UserRepositoryTest.java`

- [ ] **Step 1: 리포지토리 슬라이스 테스트 작성 (failing)**

`src/test/java/com/ysk/contact/repository/UserRepositoryTest.java`:
```java
package com.ysk.contact.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.ysk.contact.entity.User;
import com.ysk.contact.support.IntegrationTest;

class UserRepositoryTest extends IntegrationTest {

    @Autowired
    UserRepository userRepository;

    @Test
    void savesAndFindsByUsername() {
        userRepository.save(User.builder().username("alice").password("hash").build());

        assertThat(userRepository.findByUsername("alice")).isPresent();
        assertThat(userRepository.existsByUsername("alice")).isTrue();
        assertThat(userRepository.existsByUsername("bob")).isFalse();
    }
}
```

- [ ] **Step 2: 테스트 실행 → 컴파일 실패 확인**

Run: `./gradlew test --tests "com.ysk.contact.repository.UserRepositoryTest"`
Expected: **FAIL** — `User`, `UserRepository` 미존재로 컴파일 에러

- [ ] **Step 3: User 엔티티 작성**

`src/main/java/com/ysk/contact/entity/User.java`:
```java
package com.ysk.contact.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 4: UserRepository 작성**

`src/main/java/com/ysk/contact/repository/UserRepository.java`:
```java
package com.ysk.contact.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ysk.contact.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
}
```

- [ ] **Step 5: 테스트 실행 → 통과 확인**

Run: `./gradlew test --tests "com.ysk.contact.repository.UserRepositoryTest"`
Expected: **PASS**

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/ysk/contact/entity/User.java src/main/java/com/ysk/contact/repository/UserRepository.java src/test/java/com/ysk/contact/repository/UserRepositoryTest.java
git commit -m "feat(backend): User 엔티티 + UserRepository"
```

---

## Task 3: 보안 기반 (BCrypt, UserDetailsService, SecurityConfig, 예외 핸들러)

**Files:**
- Create: `src/main/java/com/ysk/contact/config/PasswordEncoderConfig.java`
- Create: `src/main/java/com/ysk/contact/service/CustomUserDetailsService.java`
- Create: `src/main/java/com/ysk/contact/config/SecurityConfig.java`
- Create: `src/main/java/com/ysk/contact/config/GlobalExceptionHandler.java`

> 이 Task는 인프라 구성이라 단독 테스트보다 Task 4·5의 API 테스트로 검증한다. 각 파일 작성 후 컴파일만 확인하고 커밋한다.

- [ ] **Step 1: BCrypt 인코더 빈 작성**

`src/main/java/com/ysk/contact/config/PasswordEncoderConfig.java`:
```java
package com.ysk.contact.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

- [ ] **Step 2: CustomUserDetailsService 작성**

`src/main/java/com/ysk/contact/service/CustomUserDetailsService.java`:
```java
package com.ysk.contact.service;

import java.util.Collections;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ysk.contact.entity.User;
import com.ysk.contact.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
```

- [ ] **Step 3: SecurityConfig 작성**

`src/main/java/com/ysk/contact/config/SecurityConfig.java`:
```java
package com.ysk.contact.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers("/api/auth/**")
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/assets/**", "/", "/index.html", "/favicon.ico").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
```

- [ ] **Step 4: GlobalExceptionHandler 작성**

`src/main/java/com/ysk/contact/config/GlobalExceptionHandler.java`:
```java
package com.ysk.contact.config;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("잘못된 요청입니다.");
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArg(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
}
```

- [ ] **Step 5: 컴파일 확인**

Run: `./gradlew compileJava`
Expected: **BUILD SUCCESSFUL**

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/ysk/contact/config src/main/java/com/ysk/contact/service/CustomUserDetailsService.java
git commit -m "feat(backend): 보안 기반(BCrypt, UserDetailsService, SecurityConfig, 예외핸들러)"
```

---

## Task 4: 회원가입 (DTO + UserService + AuthController.register)

**Files:**
- Create: `src/main/java/com/ysk/contact/dto/RegisterRequest.java`
- Create: `src/main/java/com/ysk/contact/dto/UserResponse.java`
- Create: `src/main/java/com/ysk/contact/service/UserService.java`
- Create: `src/main/java/com/ysk/contact/controller/AuthController.java`
- Test: `src/test/java/com/ysk/contact/controller/AuthControllerTest.java`

- [ ] **Step 1: 회원가입 테스트 작성 (failing)**

`src/test/java/com/ysk/contact/controller/AuthControllerTest.java`:
```java
package com.ysk.contact.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.ysk.contact.support.IntegrationTest;

class AuthControllerTest extends IntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void register_success() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"secret12\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void register_duplicate_returns400() throws Exception {
        String body = "{\"username\":\"bob\",\"password\":\"secret12\"}";
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void register_blankUsername_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"secret12\"}"))
                .andExpect(status().isBadRequest());
    }
}
```

- [ ] **Step 2: 테스트 실행 → 실패 확인**

Run: `./gradlew test --tests "com.ysk.contact.controller.AuthControllerTest"`
Expected: **FAIL** — `RegisterRequest`/`UserService`/`AuthController` 미존재 컴파일 에러

- [ ] **Step 3: DTO 작성**

`src/main/java/com/ysk/contact/dto/RegisterRequest.java`:
```java
package com.ysk.contact.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "사용자명은 필수입니다.")
        @Size(min = 3, max = 30, message = "사용자명은 3~30자여야 합니다.")
        String username,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 72, message = "비밀번호는 8~72자여야 합니다.")
        String password
) {}
```

`src/main/java/com/ysk/contact/dto/UserResponse.java`:
```java
package com.ysk.contact.dto;

import java.time.LocalDateTime;

import com.ysk.contact.entity.User;

public record UserResponse(Long id, String username, LocalDateTime createdAt) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getCreatedAt());
    }
}
```

- [ ] **Step 4: UserService 작성**

`src/main/java/com/ysk/contact/service/UserService.java`:
```java
package com.ysk.contact.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ysk.contact.entity.User;
import com.ysk.contact.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User register(String username, String rawPassword) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("이미 사용 중인 사용자명입니다.");
        }
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(rawPassword))
                .build();
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username));
    }
}
```

- [ ] **Step 5: AuthController 작성 (register만)**

`src/main/java/com/ysk/contact/controller/AuthController.java`:
```java
package com.ysk.contact.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ysk.contact.dto.RegisterRequest;
import com.ysk.contact.dto.UserResponse;
import com.ysk.contact.entity.User;
import com.ysk.contact.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request.username(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }
}
```

- [ ] **Step 6: 테스트 실행 → 통과 확인**

Run: `./gradlew test --tests "com.ysk.contact.controller.AuthControllerTest"`
Expected: **PASS** (register 3개 테스트)

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/ysk/contact/dto src/main/java/com/ysk/contact/service/UserService.java src/main/java/com/ysk/contact/controller/AuthController.java src/test/java/com/ysk/contact/controller/AuthControllerTest.java
git commit -m "feat(backend): 회원가입 API(/api/auth/register) + 검증"
```

---

## Task 5: 로그인 / 로그아웃 / 현재 사용자(me)

**Files:**
- Create: `src/main/java/com/ysk/contact/dto/LoginRequest.java`
- Modify: `src/main/java/com/ysk/contact/controller/AuthController.java`
- Modify: `src/test/java/com/ysk/contact/controller/AuthControllerTest.java`

- [ ] **Step 1: 로그인/me 테스트 추가 (failing)**

`src/test/java/com/ysk/contact/controller/AuthControllerTest.java` 의 클래스 안에 메서드 추가:
```java
    @Test
    void login_then_me_success() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"carol\",\"password\":\"secret12\"}"))
                .andExpect(status().isCreated());

        var session = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"carol\",\"password\":\"secret12\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("carol"))
                .andReturn().getRequest().getSession(false);

        mockMvc.perform(get("/api/auth/me").session(
                        (org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("carol"));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"dave\",\"password\":\"secret12\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"dave\",\"password\":\"wrongpass\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withoutLogin_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }
```

그리고 import 추가(파일 상단):
```java
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
```

- [ ] **Step 2: 테스트 실행 → 실패 확인**

Run: `./gradlew test --tests "com.ysk.contact.controller.AuthControllerTest"`
Expected: **FAIL** — login/me 핸들러 없음(404) 및 `LoginRequest` 미존재

- [ ] **Step 3: LoginRequest 작성**

`src/main/java/com/ysk/contact/dto/LoginRequest.java`:
```java
package com.ysk.contact.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password
) {}
```

- [ ] **Step 4: AuthController에 login/logout/me 추가**

`src/main/java/com/ysk/contact/controller/AuthController.java` 를 아래로 교체:
```java
package com.ysk.contact.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ysk.contact.dto.LoginRequest;
import com.ysk.contact.dto.RegisterRequest;
import com.ysk.contact.dto.UserResponse;
import com.ysk.contact.entity.User;
import com.ysk.contact.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String SPRING_SECURITY_CONTEXT_KEY = "SPRING_SECURITY_CONTEXT";

    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request.username(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            HttpSession session = httpRequest.getSession(true);
            session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, context);

            User user = userService.findByUsername(request.username());
            return ResponseEntity.ok(UserResponse.from(user));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userService.findByUsername(authentication.getName());
        return ResponseEntity.ok(UserResponse.from(user));
    }
}
```

- [ ] **Step 5: 테스트 실행 → 통과 확인**

Run: `./gradlew test --tests "com.ysk.contact.controller.AuthControllerTest"`
Expected: **PASS** (register 3 + login/me 3 = 6개)

> 참고: `/api/auth/me` 미인증 401 검증은 SecurityConfig에서 `/api/auth/**` 가 permitAll 이지만, 핸들러 내부에서 인증 컨텍스트가 없으면 직접 401을 반환하므로 통과한다.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/ysk/contact/dto/LoginRequest.java src/main/java/com/ysk/contact/controller/AuthController.java src/test/java/com/ysk/contact/controller/AuthControllerTest.java
git commit -m "feat(backend): 로그인/로그아웃/me(세션 기반 인증)"
```

---

## Task 6: 전체 테스트 + 로컬 수동 검증 + 마무리

**Files:** 없음(검증 단계)

- [ ] **Step 1: 전체 테스트 실행**

Run: `./gradlew test`
Expected: **BUILD SUCCESSFUL** — ContactApplicationTests + UserRepositoryTest + AuthControllerTest 전부 PASS

- [ ] **Step 2: (Docker 있으면) 로컬 실 DB 수동 검증**

SSH 터널을 연 상태에서 local profile로 기동:
```bash
./gradlew bootRun
```
다른 터미널에서 `python scripts/db_test.py` 재실행 → `users` 테이블이 생성됐는지 확인(테이블 목록에 `users` 등장).
회원가입 호출:
```bash
curl -s -X POST http://localhost:8181/api/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"tester\",\"password\":\"secret12\"}"
```
Expected: 201 + `{"id":...,"username":"tester","createdAt":...}` (password 없음)

- [ ] **Step 3: 최종 커밋**

```bash
git add -A
git commit -m "chore(backend): M1 백엔드 부트스트랩 + 인증 완료"
git push origin main
```

---

## Self-Review (작성자 체크)

**1. 스펙 커버리지** (설계 문서 대비)
- 회원가입/로그인/로그아웃/현재 사용자 조회 → Task 4·5 ✅
- 세션 기반 인증 + CSRF 쿠키 토큰 → Task 3 SecurityConfig ✅
- 비밀번호 BCrypt → Task 3 PasswordEncoderConfig + Task 4 UserService ✅
- 입력 검증(이메일 형식은 M2 Contact에서, 여기선 username/password 필수·길이) → Task 4 RegisterRequest ✅
- MariaDB 연결 → Task 1 + Task 6 수동 검증 ✅
- 전역 공유 데이터(Contact)는 M2 범위 — M1 해당 없음 ✅

**2. 플레이스홀더 스캔:** "TODO/적절히/유사하게" 없음. 모든 step에 실제 코드/명령 포함 ✅

**3. 타입 일관성:**
- `UserResponse.from(User)` Task 4 정의 → Task 5 login/me에서 동일 사용 ✅
- `userService.register(username, password)` / `findByUsername(username)` 시그니처 Task 4 정의 → Task 5 사용 일치 ✅
- `RegisterRequest.username()/password()`, `LoginRequest.username()/password()` record 접근자 일치 ✅
- 세션 키 상수 `SPRING_SECURITY_CONTEXT` Task 5에서 정의·사용 ✅

**주의/리스크:**
- CSRF: `/api/auth/**` 는 ignore라 테스트·초기 호출 단순. 그 외 `/api/**`(M2 Contact 변경 요청)은 CSRF 토큰 필요 → 프론트(M3)에서 `apiFetch`가 `X-XSRF-TOKEN` 첨부.
- Testcontainers는 Docker 필수. CI/로컬에 Docker 없으면 통합 테스트 스킵 사유를 명시.
- `gradle wrapper` 생성에 로컬 gradle 또는 IDE 필요(Task 1 Step 6).
