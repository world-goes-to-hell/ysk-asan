package com.ysk.contact.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.ysk.contact.support.IntegrationTest;

import jakarta.servlet.http.Cookie;

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

    @Test
    void logout_invalidatesSession_thenMeReturns401() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"erin\",\"password\":\"secret12\"}"))
                .andExpect(status().isCreated());

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

    // ---------- 로그인 유지(Remember-Me) ----------

    private void register(String username) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"secret12\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void login_withRememberMe_setsRememberMeCookie() throws Exception {
        register("rmcookie");

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
        register("rmonly");

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
        register("normemb");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"normemb\",\"password\":\"secret12\",\"rememberMe\":false}"))
                .andExpect(status().isOk())
                .andExpect(cookie().doesNotExist("remember-me"));
    }

    @Test
    void logout_clearsRememberMeCookie() throws Exception {
        register("rmlogout");

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
