package com.ysk.contact.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import com.ysk.contact.entity.UserRole;
import com.ysk.contact.repository.UserRepository;
import com.ysk.contact.support.IntegrationTest;

class AdminUserControllerTest extends IntegrationTest {

    private static final String BASE = "/api/admin/users";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    private String credentials(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    /** 가입만(승인 대기). 생성된 id 반환. */
    private long register(String username) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username, "secret12")))
                .andExpect(status().isCreated());
        return userRepository.findByUsername(username).orElseThrow().getId();
    }

    /** 가입 + 승인. */
    private long registerApproved(String username) throws Exception {
        long id = register(username);
        var user = userRepository.findByUsername(username).orElseThrow();
        user.approve();
        // 테스트 트랜잭션의 flush 타이밍에 의존하지 않도록 명시 저장.
        userRepository.save(user);
        return id;
    }

    /** 로그인 세션 생성(승인된 계정 전제). */
    private MockHttpSession login(String username) throws Exception {
        return (MockHttpSession) mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username, "secret12")))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }

    /** 가입 + 승인 + ADMIN 승격 + 로그인 세션. */
    private MockHttpSession adminSession(String username) throws Exception {
        register(username);
        var admin = userRepository.findByUsername(username).orElseThrow();
        admin.approve();
        admin.changeRole(UserRole.ADMIN);
        userRepository.save(admin);
        return login(username);
    }

    // ---------- 인가 가드 ----------

    @Test
    void list_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
    }

    @Test
    void list_asNonAdmin_returns403() throws Exception {
        registerApproved("plainuser");
        MockHttpSession session = login("plainuser");

        mockMvc.perform(get(BASE).session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void approve_asNonAdmin_returns403() throws Exception {
        registerApproved("plainuser2");
        long targetId = register("target0");
        MockHttpSession session = login("plainuser2");

        mockMvc.perform(post(BASE + "/" + targetId + "/approve").session(session).with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ---------- 회원 목록 ----------

    @Test
    void list_asAdmin_returnsUsersWithStatus() throws Exception {
        MockHttpSession session = adminSession("boss");
        register("newbie");

        mockMvc.perform(get(BASE).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.username=='newbie')].approved").value(false))
                .andExpect(jsonPath("$[?(@.username=='newbie')].role").value("USER"))
                .andExpect(jsonPath("$[?(@.username=='boss')].role").value("ADMIN"));
    }

    // ---------- 승인 ----------

    @Test
    void approve_thenUserCanLogin() throws Exception {
        MockHttpSession session = adminSession("boss2");
        long targetId = register("waiting");

        // 승인 전 로그인 불가
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("waiting", "secret12")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post(BASE + "/" + targetId + "/approve").session(session).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approved").value(true));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("waiting", "secret12")))
                .andExpect(status().isOk());
    }

    @Test
    void approve_missingUser_returns404() throws Exception {
        MockHttpSession session = adminSession("boss3");

        mockMvc.perform(post(BASE + "/999999/approve").session(session).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    // ---------- 잠금 해제 ----------

    @Test
    void unlock_resetsLockAndFailures_thenUserCanLogin() throws Exception {
        MockHttpSession session = adminSession("boss4");
        long targetId = registerApproved("locky");

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(credentials("locky", "wrongpass")))
                    .andExpect(status().isUnauthorized());
        }
        assertThat(userRepository.findByUsername("locky").orElseThrow().isLocked()).isTrue();

        mockMvc.perform(post(BASE + "/" + targetId + "/unlock").session(session).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locked").value(false))
                .andExpect(jsonPath("$.failedLoginAttempts").value(0));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("locky", "secret12")))
                .andExpect(status().isOk());
    }

    // ---------- 권한 변경 ----------

    @Test
    void changeRole_promotesToAdmin() throws Exception {
        MockHttpSession session = adminSession("boss5");
        long targetId = registerApproved("promotee");

        mockMvc.perform(patch(BASE + "/" + targetId + "/role").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        assertThat(userRepository.findByUsername("promotee").orElseThrow().getRole())
                .isEqualTo(UserRole.ADMIN);
    }

    @Test
    void changeRole_invalidRole_returns400() throws Exception {
        MockHttpSession session = adminSession("boss7");
        long targetId = registerApproved("victim");

        // 정의되지 않은 enum 은 역직렬화(HttpMessageNotReadable) 단계에서 400.
        mockMvc.perform(patch(BASE + "/" + targetId + "/role").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"SUPERADMIN\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changeRole_self_returns400() throws Exception {
        MockHttpSession session = adminSession("boss6");
        long selfId = userRepository.findByUsername("boss6").orElseThrow().getId();

        mockMvc.perform(patch(BASE + "/" + selfId + "/role").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"USER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
}
