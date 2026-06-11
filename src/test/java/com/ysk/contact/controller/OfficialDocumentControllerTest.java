package com.ysk.contact.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ysk.contact.support.IntegrationTest;

class OfficialDocumentControllerTest extends IntegrationTest {

    private static final String BASE = "/api/documents";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    private String issueBody(String templateId, String password) {
        return "{\"templateId\":\"" + templateId + "\",\"password\":\"" + password
                + "\",\"fields\":{\"title\":\"협조 요청\",\"receiver\":\"행정실\"}}";
    }

    /** 공문 발급 후 토큰 반환(발급은 로그인 사용자 전용 — @WithMockUser 필요). */
    private String issue(String password) throws Exception {
        MvcResult result = mockMvc.perform(post(BASE).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(issueBody("general-official", password)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private String passwordBody(String password) {
        return "{\"password\":\"" + password + "\"}";
    }

    // ---------- 발급 (4·5차) ----------

    @Test
    void issue_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post(BASE).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(issueBody("general-official", "view1234")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "issuer1")
    void issue_returnsHighEntropyToken() throws Exception {
        String token = issue("view1234");
        // 256bit SecureRandom → base64url 43자(패딩 없음)
        assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]+");
    }

    @Test
    @WithMockUser
    void issue_unknownTemplate_returns400() throws Exception {
        mockMvc.perform(post(BASE).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(issueBody("no-such-template", "view1234")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    // ---------- 열람 (6·7차) ----------

    @Test
    @WithMockUser
    void view_correctPassword_returnsFields() throws Exception {
        String token = issue("view1234");

        mockMvc.perform(post(BASE + "/" + token + "/view").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordBody("view1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.templateId").value("general-official"))
                .andExpect(jsonPath("$.fields.title").value("협조 요청"))
                .andExpect(jsonPath("$.fields.receiver").value("행정실"))
                .andExpect(jsonPath("$.sealImageBase64").doesNotExist());
    }

    @Test
    @WithMockUser
    void view_wrongPassword_returns401() throws Exception {
        String token = issue("view1234");

        mockMvc.perform(post(BASE + "/" + token + "/view").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordBody("wrongpw99")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void view_unknownToken_returns404() throws Exception {
        mockMvc.perform(post(BASE + "/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA/view").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordBody("view1234")))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void view_afterTenFailures_locksDocument() throws Exception {
        String token = issue("view1234");

        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post(BASE + "/" + token + "/view").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(passwordBody("wrongpw99")))
                    .andExpect(status().isUnauthorized());
        }

        // 잠긴 뒤에는 올바른 비밀번호여도 존재 자체를 숨긴다(404 — 재발급 필요).
        mockMvc.perform(post(BASE + "/" + token + "/view").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordBody("view1234")))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void view_withoutCsrf_returns403() throws Exception {
        String token = issue("view1234");

        mockMvc.perform(post(BASE + "/" + token + "/view")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordBody("view1234")))
                .andExpect(status().isForbidden());
    }

    // ---------- 입력값 이력 자동완성 (M13) ----------

    private String issueWithFields(String fieldsJson) throws Exception {
        MvcResult result = mockMvc.perform(post(BASE).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"templateId\":\"general-official\",\"password\":\"view1234\","
                                + "\"fields\":" + fieldsJson + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void fieldHistory_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get(BASE + "/field-history").param("templateId", "general-official"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void fieldHistory_recordsShortFields_excludesMultilineAndLong() throws Exception {
        String longValue = "가".repeat(101);
        issueWithFields("{\"receiver\":\"행정실\",\"body\":\"1. 첫줄\\n2. 둘째줄\",\"title\":\""
                + longValue + "\"}");

        mockMvc.perform(get(BASE + "/field-history").param("templateId", "general-official"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiver[0]").value("행정실"))
                .andExpect(jsonPath("$.body").doesNotExist())   // 개행 포함 → 제외
                .andExpect(jsonPath("$.title").doesNotExist()); // 100자 초과 → 제외
    }

    @Test
    @WithMockUser
    void fieldHistory_duplicateValue_storedOnce_andRecencyOrdered() throws Exception {
        issueWithFields("{\"receiver\":\"행정실\"}");
        issueWithFields("{\"receiver\":\"총무팀\"}");
        issueWithFields("{\"receiver\":\"행정실\"}"); // 재사용 → 중복 행 없이 최근 사용으로 갱신

        mockMvc.perform(get(BASE + "/field-history").param("templateId", "general-official"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiver.length()").value(2))
                .andExpect(jsonPath("$.receiver[0]").value("행정실"))  // 최근 사용순
                .andExpect(jsonPath("$.receiver[1]").value("총무팀"));
    }

    @Test
    @WithMockUser
    void fieldHistory_isolatedPerTemplate() throws Exception {
        issueWithFields("{\"receiver\":\"행정실\"}"); // general-official 에만 기록

        mockMvc.perform(get(BASE + "/field-history").param("templateId", "employment-cert"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiver").doesNotExist());
    }

    @Test
    @WithMockUser
    void fieldHistory_unknownTemplate_returns400() throws Exception {
        mockMvc.perform(get(BASE + "/field-history").param("templateId", "no-such"))
                .andExpect(status().isBadRequest());
    }

    // ---------- 발급자 목록/열람 (M11) ----------

    /** 지정 사용자로 발급(요청별 인증). */
    private String issueAs(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post(BASE).with(user(username)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(issueBody("general-official", password)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void mine_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get(BASE + "/mine")).andExpect(status().isUnauthorized());
    }

    @Test
    void mine_listsOnlyOwnDocuments_withSealedFlag() throws Exception {
        String myToken = issueAs("issuerA", "view1234");
        issueAs("issuerB", "view1234"); // 타인 발급 — 목록에 나오면 안 됨

        mockMvc.perform(get(BASE + "/mine").with(user("issuerA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].token").value(myToken))
                .andExpect(jsonPath("$[0].templateId").value("general-official"))
                .andExpect(jsonPath("$[0].sealed").value(false));

        // 직인 저장 후 sealed=true 로 반영
        mockMvc.perform(multipart(BASE + "/" + myToken + "/seal")
                        .file(sealPng(new byte[] { (byte) 0x89, 'P', 'N', 'G', 1 }))
                        .param("password", "view1234")
                        .with(csrf()))
                .andExpect(status().isOk());

        mockMvc.perform(get(BASE + "/mine").with(user("issuerA")))
                .andExpect(jsonPath("$[0].sealed").value(true));
    }

    @Test
    void issuerView_owner_returnsDocumentWithoutPassword() throws Exception {
        String token = issueAs("issuerA", "view1234");

        mockMvc.perform(get(BASE + "/" + token + "/issuer-view").with(user("issuerA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.templateId").value("general-official"))
                .andExpect(jsonPath("$.fields.title").value("협조 요청"));
    }

    @Test
    void issuerView_otherUser_returns404() throws Exception {
        String token = issueAs("issuerA", "view1234");

        mockMvc.perform(get(BASE + "/" + token + "/issuer-view").with(user("issuerB")))
                .andExpect(status().isNotFound());
    }

    @Test
    void issuerView_unauthenticated_returns401() throws Exception {
        String token = issueAs("issuerA", "view1234");

        mockMvc.perform(get(BASE + "/" + token + "/issuer-view"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void seal_reupload_replacesImage() throws Exception {
        // 잘못 올린 직인을 다시 저장하면 교체되어야 한다(저장 확정 흐름의 전제).
        String token = issue("view1234");
        byte[] first = new byte[] { (byte) 0x89, 'P', 'N', 'G', 1 };
        byte[] second = new byte[] { (byte) 0x89, 'P', 'N', 'G', 2, 2 };

        mockMvc.perform(multipart(BASE + "/" + token + "/seal").file(sealPng(first))
                        .param("password", "view1234").with(csrf()))
                .andExpect(status().isOk());
        mockMvc.perform(multipart(BASE + "/" + token + "/seal").file(sealPng(second))
                        .param("password", "view1234").with(csrf()))
                .andExpect(status().isOk());

        mockMvc.perform(post(BASE + "/" + token + "/view").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordBody("view1234")))
                .andExpect(jsonPath("$.sealImageBase64").value(
                        java.util.Base64.getEncoder().encodeToString(second)));
    }

    // ---------- 직인 (8차) ----------

    private MockMultipartFile sealPng(byte[] bytes) {
        return new MockMultipartFile("file", "seal.png", "image/png", bytes);
    }

    @Test
    @WithMockUser
    void seal_upload_thenViewIncludesSealImage() throws Exception {
        String token = issue("view1234");
        byte[] png = new byte[] { (byte) 0x89, 'P', 'N', 'G', 1, 2, 3, 4 };

        mockMvc.perform(multipart(BASE + "/" + token + "/seal")
                        .file(sealPng(png))
                        .param("password", "view1234")
                        .with(csrf()))
                .andExpect(status().isOk());

        mockMvc.perform(post(BASE + "/" + token + "/view").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordBody("view1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sealImageBase64").value(
                        java.util.Base64.getEncoder().encodeToString(png)))
                .andExpect(jsonPath("$.sealContentType").value("image/png"));
    }

    @Test
    @WithMockUser
    void seal_wrongPassword_returns401_andNotStored() throws Exception {
        String token = issue("view1234");

        mockMvc.perform(multipart(BASE + "/" + token + "/seal")
                        .file(sealPng(new byte[] { 1, 2, 3 }))
                        .param("password", "wrongpw99")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post(BASE + "/" + token + "/view").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordBody("view1234")))
                .andExpect(jsonPath("$.sealImageBase64").doesNotExist());
    }

    @Test
    @WithMockUser
    void seal_spoofedContentType_returns400() throws Exception {
        // Content-Type 은 image/png 로 선언했지만 실제 바이트는 PNG 가 아님 — 매직 바이트 검증.
        String token = issue("view1234");

        mockMvc.perform(multipart(BASE + "/" + token + "/seal")
                        .file(sealPng(new byte[] { 1, 2, 3, 4, 5 }))
                        .param("password", "view1234")
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void seal_nonImageContentType_returns400() throws Exception {
        String token = issue("view1234");
        MockMultipartFile pdf = new MockMultipartFile(
                "file", "seal.pdf", "application/pdf", new byte[] { 1, 2, 3 });

        mockMvc.perform(multipart(BASE + "/" + token + "/seal")
                        .file(pdf)
                        .param("password", "view1234")
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void seal_overOneMegabyte_returns400() throws Exception {
        String token = issue("view1234");
        byte[] big = new byte[1024 * 1024 + 1];

        mockMvc.perform(multipart(BASE + "/" + token + "/seal")
                        .file(sealPng(big))
                        .param("password", "view1234")
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }
}
