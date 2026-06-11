package com.ysk.contact.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

    // ---------- CSV 가져오기 ----------

    private static final String IMPORT_HEADER = "부서,이름,이메일";

    private org.springframework.mock.web.MockMultipartFile csvFile(byte[] bytes) {
        return new org.springframework.mock.web.MockMultipartFile(
                "file", "contacts.csv", "text/csv", bytes);
    }

    private byte[] utf8Bom(String csv) {
        byte[] body = csv.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] withBom = new byte[body.length + 3];
        withBom[0] = (byte) 0xEF;
        withBom[1] = (byte) 0xBB;
        withBom[2] = (byte) 0xBF;
        System.arraycopy(body, 0, withBom, 3, body.length);
        return withBom;
    }

    @Test
    void import_unauthenticated_returns401() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart(BASE + "/import").file(csvFile(utf8Bom(IMPORT_HEADER + "\r\n"))).with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void import_withoutCsrf_returns403() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart(BASE + "/import").file(csvFile(utf8Bom(IMPORT_HEADER + "\r\n"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void import_utf8Bom_registersRows() throws Exception {
        String csv = IMPORT_HEADER + "\r\n영업,홍길동,hong@ex.com\r\n개발,김철수,kim@ex.com\r\n";

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart(BASE + "/import").file(csvFile(utf8Bom(csv))).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(2));

        mockMvc.perform(get(BASE))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.name=='홍길동')].department").value("영업"));
    }

    @Test
    @WithMockUser
    void import_cp949_registersKoreanRows() throws Exception {
        // 한국어 Excel 의 레거시 "CSV(쉼표로 분리)" 저장은 BOM 없는 CP949 — 폴백 디코딩 검증.
        String csv = IMPORT_HEADER + "\r\n영업,홍길동,hong@ex.com\r\n";
        byte[] cp949 = csv.getBytes(java.nio.charset.Charset.forName("MS949"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart(BASE + "/import").file(csvFile(cp949)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(1));

        mockMvc.perform(get(BASE))
                .andExpect(jsonPath("$[0].name").value("홍길동"));
    }

    @Test
    @WithMockUser
    void import_invalidRow_returns400WithRowErrors_andNothingImported() throws Exception {
        // 3행(데이터 2행째)의 이메일 형식 오류 — all-or-nothing: 1건도 등록되지 않아야 한다.
        String csv = IMPORT_HEADER + "\r\n영업,홍길동,hong@ex.com\r\n개발,김철수,not-an-email\r\n";

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart(BASE + "/import").file(csvFile(utf8Bom(csv))).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.errors[0].row").value(3))
                .andExpect(jsonPath("$.errors[0].message").exists());

        mockMvc.perform(get(BASE)).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser
    void import_wrongHeader_returns400() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart(BASE + "/import")
                        .file(csvFile(utf8Bom("이름,부서,이메일\r\n홍길동,영업,hong@ex.com\r\n"))).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("부서,이름,이메일")));
    }

    @Test
    @WithMockUser
    void import_quotedFields_andGuardPrefix_roundTrip() throws Exception {
        // M7 내보내기 산출물 형태: 쿼팅 + 수식 가드(') — 가져오기 시 원본 값으로 복원돼야 한다.
        String csv = IMPORT_HEADER + "\r\n\"영업,\"\"1\"\"팀\",'=SUM(A1:A9),x@ex.com\r\n";

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart(BASE + "/import").file(csvFile(utf8Bom(csv))).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(1));

        mockMvc.perform(get(BASE))
                .andExpect(jsonPath("$[0].department").value("영업,\"1\"팀"))
                .andExpect(jsonPath("$[0].name").value("=SUM(A1:A9)"));
    }

    @Test
    @WithMockUser
    void import_emptyFile_returns400() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart(BASE + "/import").file(csvFile(new byte[0])).with(csrf()))
                .andExpect(status().isBadRequest());
    }

    // ---------- 변경 이력(작성자/수정자) ----------

    @Test
    @WithMockUser(username = "kimwriter")
    void create_recordsCreatedByAndUpdatedBy() throws Exception {
        mockMvc.perform(post(BASE).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body("영업", "홍길동", "hong@ex.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdBy").value("kimwriter"))
                .andExpect(jsonPath("$.updatedBy").value("kimwriter"));
    }

    @Test
    void update_byDifferentUser_changesUpdatedBy_keepsCreatedBy() throws Exception {
        // 요청별 인증(with(user)) 으로 작성자/수정자를 다른 사람으로 분리해 검증한다.
        MvcResult created = mockMvc.perform(post(BASE)
                        .with(org.springframework.security.test.web.servlet.request
                                .SecurityMockMvcRequestPostProcessors.user("kimwriter"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body("영업", "홍길동", "hong@ex.com")))
                .andExpect(status().isCreated())
                .andReturn();
        long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put(BASE + "/" + id)
                        .with(org.springframework.security.test.web.servlet.request
                                .SecurityMockMvcRequestPostProcessors.user("leeeditor"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body("개발", "홍길동", "hong@ex.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdBy").value("kimwriter"))   // 작성자 유지(updatable=false)
                .andExpect(jsonPath("$.updatedBy").value("leeeditor")); // 수정자 갱신
    }

    // ---------- CSV 내보내기 ----------

    @Test
    void export_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get(BASE + "/export")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void export_returnsCsv_withBomHeaderAndRows() throws Exception {
        createContact("영업", "홍길동", "hong@ex.com");
        createContact("개발", "김철수", "kim@ex.com");

        MvcResult result = mockMvc.perform(get(BASE + "/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        org.hamcrest.Matchers.containsString("text/csv")))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("attachment")))
                .andReturn();

        String csv = result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        // Excel 한글 인식용 UTF-8 BOM 이 선두에 있어야 한다.
        org.assertj.core.api.Assertions.assertThat(csv).startsWith("\uFEFF");
        org.assertj.core.api.Assertions.assertThat(csv).contains("부서,이름,이메일");
        org.assertj.core.api.Assertions.assertThat(csv).contains("영업,홍길동,hong@ex.com");
        org.assertj.core.api.Assertions.assertThat(csv).contains("개발,김철수,kim@ex.com");
    }

    @Test
    @WithMockUser
    void export_respectsDepartmentFilter() throws Exception {
        createContact("영업", "홍길동", "hong@ex.com");
        createContact("개발", "김철수", "kim@ex.com");

        String csv = mockMvc.perform(get(BASE + "/export").param("department", "영업"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);

        org.assertj.core.api.Assertions.assertThat(csv).contains("홍길동");
        org.assertj.core.api.Assertions.assertThat(csv).doesNotContain("김철수");
    }

    @Test
    @WithMockUser
    void export_escapesSpecialChars_andGuardsFormulaInjection() throws Exception {
        // 쉼표/따옴표 포함 부서, 수식 시작(=) 이름 — RFC4180 쿼팅 + 수식 인젝션 가드 검증.
        // 따옴표가 body() 의 문자열 연결 JSON 을 깨뜨리므로 ObjectMapper 로 안전 직렬화.
        String json = objectMapper.writeValueAsString(java.util.Map.of(
                "department", "영업,\"1\"팀",
                "name", "=SUM(A1:A9)",
                "email", "evil@ex.com"));
        mockMvc.perform(post(BASE).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());

        String csv = mockMvc.perform(get(BASE + "/export"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);

        // 쉼표·따옴표 포함 필드는 쿼팅되고 내부 따옴표는 이중화된다.
        org.assertj.core.api.Assertions.assertThat(csv).contains("\"영업,\"\"1\"\"팀\"");
        // 수식 시작 문자는 작은따옴표 prefix 로 무력화된다(엑셀이 텍스트로 취급).
        org.assertj.core.api.Assertions.assertThat(csv).contains("'=SUM(A1:A9)");
        org.assertj.core.api.Assertions.assertThat(csv).doesNotContain(",=SUM");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(
            strings = { "=SUM(A1)", "+1+1", "-1+1", "@SUM(A1)", "|cmd /c calc" })
    @WithMockUser
    void export_guardsAllInjectionPrefixes(String injectedName) throws Exception {
        // OWASP CSV Injection 시작 문자 전부에 ' prefix 가 붙는지 회귀 가드.
        String json = objectMapper.writeValueAsString(java.util.Map.of(
                "department", "영업", "name", injectedName, "email", "x@ex.com"));
        mockMvc.perform(post(BASE).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());

        String csv = mockMvc.perform(get(BASE + "/export"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);

        org.assertj.core.api.Assertions.assertThat(csv).contains("'" + injectedName);
        org.assertj.core.api.Assertions.assertThat(csv).doesNotContain("," + injectedName);
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
