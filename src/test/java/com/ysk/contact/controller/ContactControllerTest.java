package com.ysk.contact.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ysk.contact.support.IntegrationTest;

class ContactControllerTest extends IntegrationTest {

    private static final String BASE = "/api/contacts";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    private String body(String department, String name, String email) {
        return "{\"department\":\"" + department + "\",\"name\":\"" + name
                + "\",\"email\":\"" + email + "\"}";
    }

    /** 연락처를 추가하고 생성된 id 를 반환한다(@Transactional rollback 으로 id 비연속 → 항상 캡처). */
    private long createContact(String department, String name, String email) throws Exception {
        MvcResult result = mockMvc.perform(post(BASE).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body(department, name, email)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    // ---------- 인증/CSRF 가드 ----------

    @Test
    void list_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
    }

    @Test
    void create_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post(BASE).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body("영업", "홍길동", "h@ex.com")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void create_withoutCsrf_returns403() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON).content(body("영업", "홍길동", "h@ex.com")))
                .andExpect(status().isForbidden());
    }

    // ---------- 생성 ----------

    @Test
    @WithMockUser
    void create_success_returns201WithBody() throws Exception {
        mockMvc.perform(post(BASE).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body("영업", "홍길동", "hong@ex.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.department").value("영업"))
                .andExpect(jsonPath("$.name").value("홍길동"))
                .andExpect(jsonPath("$.email").value("hong@ex.com"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @WithMockUser
    void create_blankName_returns400() throws Exception {
        mockMvc.perform(post(BASE).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body("영업", "", "hong@ex.com")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @WithMockUser
    void create_blankDepartment_returns400() throws Exception {
        mockMvc.perform(post(BASE).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body("", "홍길동", "hong@ex.com")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void create_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post(BASE).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body("영업", "홍길동", "not-an-email")))
                .andExpect(status().isBadRequest());
    }

    // ---------- 수정 ----------

    @Test
    @WithMockUser
    void update_success_returns200WithUpdatedFields() throws Exception {
        long id = createContact("영업", "홍길동", "hong@ex.com");

        mockMvc.perform(put(BASE + "/" + id).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body("개발", "홍길순", "hong2@ex.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.department").value("개발"))
                .andExpect(jsonPath("$.name").value("홍길순"))
                .andExpect(jsonPath("$.email").value("hong2@ex.com"));
    }

    @Test
    @WithMockUser
    void update_missing_returns404() throws Exception {
        mockMvc.perform(put(BASE + "/999999").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body("영업", "홍길동", "hong@ex.com")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    // ---------- 조회/검색/부서 ----------

    @Test
    @WithMockUser
    void list_filtersByDepartmentAndQuery() throws Exception {
        createContact("영업", "Alice", "alice@ex.com");
        createContact("개발", "Bob", "bob@ex.com");

        mockMvc.perform(get(BASE).param("department", "영업"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Alice"));

        mockMvc.perform(get(BASE).param("q", "bob"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Bob"));
    }

    @Test
    @WithMockUser
    void list_emptyQuery_returnsAll() throws Exception {
        createContact("영업", "Alice", "alice@ex.com");
        createContact("개발", "Bob", "bob@ex.com");

        mockMvc.perform(get(BASE).param("q", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @WithMockUser
    void departments_returnsDistinctSorted() throws Exception {
        createContact("영업", "a", "a@ex.com");
        createContact("영업", "b", "b@ex.com");
        createContact("개발", "c", "c@ex.com");

        mockMvc.perform(get(BASE + "/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0]").value("개발"))
                .andExpect(jsonPath("$[1]").value("영업"));
    }

    // ---------- 일괄 삭제 ----------

    @Test
    @WithMockUser
    void delete_bulk_returnsDeletedCount() throws Exception {
        long id1 = createContact("영업", "a", "a@ex.com");
        long id2 = createContact("개발", "b", "b@ex.com");

        mockMvc.perform(delete(BASE).with(csrf()).param("ids", String.valueOf(id1), String.valueOf(id2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(2));

        mockMvc.perform(get(BASE))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser
    void delete_missingIdsParam_returns400() throws Exception {
        mockMvc.perform(delete(BASE).with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void delete_emptyIds_returns400() throws Exception {
        mockMvc.perform(delete(BASE).with(csrf()).param("ids", ""))
                .andExpect(status().isBadRequest());
    }
}
