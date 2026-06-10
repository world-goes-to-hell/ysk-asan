package com.ysk.contact.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.ysk.contact.repository.UserRepository;
import com.ysk.contact.support.IntegrationTest;

import jakarta.servlet.http.Cookie;

class AuthControllerTest extends IntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    /** 가입 요청 본문. */
    private String credentials(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    /** 가입만 수행(승인 대기 상태). */
    private void register(String username) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username, "secret12")))
                .andExpect(status().isCreated());
    }

    /** 가입 + 관리자 승인까지 완료(로그인 가능한 계정 — 대부분의 테스트 전제). */
    private void registerApproved(String username) throws Exception {
        register(username);
        var user = userRepository.findByUsername(username).orElseThrow();
        user.approve();
        // 테스트 트랜잭션의 flush 타이밍에 의존하지 않도록 명시 저장.
        userRepository.save(user);
    }

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

    @Test
    void malformedJson_returns400_not500() throws Exception {
        // ResponseEntityExceptionHandler 상속으로 Spring MVC 표준 예외(HttpMessageNotReadable)가
        // 500 으로 변질되지 않고 4xx 로 유지되는지에 대한 회귀 가드.
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not valid json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void securityHeaders_present() throws Exception {
        // 클릭재킹/스니핑 방지 헤더가 모든 응답(미인증 401 포함)에 실리는지 확인.
        // HSTS 는 secure 요청에만 나가므로(http 테스트) 검증 대상에서 제외.
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void login_then_me_success() throws Exception {
        registerApproved("carol");

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
        registerApproved("dave");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"dave\",\"password\":\"wrongpass\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void me_withoutLogin_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_invalidatesSession_thenMeReturns401() throws Exception {
        registerApproved("erin");

        var session = (org.springframework.mock.web.MockHttpSession) mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\":\"erin\",\"password\":\"secret12\"}"))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);

        mockMvc.perform(post("/api/auth/logout").session(session).with(csrf()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    // ---------- 승인제 + 로그인 실패 잠금 ----------

    @Test
    void login_unapproved_returns401WithPendingMessage() throws Exception {
        register("pending1"); // 승인 없이 가입만

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("pending1", "secret12")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("승인")));
    }

    @Test
    void login_fiveWrongPasswords_locksAccount() throws Exception {
        registerApproved("lockme");

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(credentials("lockme", "wrongpass")))
                    .andExpect(status().isUnauthorized());
        }

        // 잠긴 뒤에는 올바른 비밀번호로도 로그인 불가 + 잠금 안내.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("lockme", "secret12")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("잠")));

        var user = userRepository.findByUsername("lockme").orElseThrow();
        assertThat(user.isLocked()).isTrue();
        assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
    }

    @Test
    void login_failureCounter_resetsOnSuccess() throws Exception {
        registerApproved("almostlock");

        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(credentials("almostlock", "wrongpass")))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("almostlock", "secret12")))
                .andExpect(status().isOk());

        var user = userRepository.findByUsername("almostlock").orElseThrow();
        assertThat(user.isLocked()).isFalse();
        assertThat(user.getFailedLoginAttempts()).isZero();
    }

    @Test
    void lockedAccount_rememberMeCookie_rejected() throws Exception {
        registerApproved("rmlocked");

        Cookie rm = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"rmlocked\",\"password\":\"secret12\",\"rememberMe\":true}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("remember-me");

        // 5회 실패로 잠금 — 기존 remember-me 쿠키도 AccountStatusUserDetailsChecker 가 거부해야 한다.
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(credentials("rmlocked", "wrongpass")))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(get("/api/auth/me").cookie(rm))
                .andExpect(status().isUnauthorized());
    }

    // ---------- 로그인 유지(Remember-Me) ----------

    @Test
    void login_withRememberMe_setsRememberMeCookie() throws Exception {
        registerApproved("rmcookie");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"rmcookie\",\"password\":\"secret12\",\"rememberMe\":true}"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("remember-me"))
                .andReturn();

        Cookie rm = result.getResponse().getCookie("remember-me");
        assertThat(rm).isNotNull();
        assertThat(rm.getValue()).isNotBlank();
        assertThat(rm.getMaxAge()).isGreaterThan(0);
    }

    @Test
    void rememberMeCookie_authenticatesWithoutSession() throws Exception {
        registerApproved("rmonly");

        Cookie rm = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"rmonly\",\"password\":\"secret12\",\"rememberMe\":true}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("remember-me");

        // 세션 없이 remember-me 쿠키만으로 인증되어야 한다(브라우저 종료·서버 재시작 모사).
        mockMvc.perform(get("/api/auth/me").cookie(rm))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("rmonly"));
    }

    @Test
    void login_withoutRememberMe_noCookie() throws Exception {
        registerApproved("normemb");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"normemb\",\"password\":\"secret12\",\"rememberMe\":false}"))
                .andExpect(status().isOk())
                .andExpect(cookie().doesNotExist("remember-me"));
    }

    @Test
    void logout_clearsRememberMeCookie() throws Exception {
        registerApproved("rmlogout");

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"rmlogout\",\"password\":\"secret12\",\"rememberMe\":true}"))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        Cookie rm = login.getResponse().getCookie("remember-me");

        mockMvc.perform(post("/api/auth/logout").session(session).cookie(rm).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("remember-me", 0));
    }
}
