package com.ysk.contact.support;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public abstract class IntegrationTest {

    // 통합 테스트는 SSH 터널 경유 개발서버 MariaDB 의 ysk_asan_test 스키마 사용.
    // 계정/비밀번호는 application-local.yml(local profile) 값을 그대로 사용하고,
    // DB 이름과 ddl-auto 만 테스트용으로 덮는다.
    @DynamicPropertySource
    static void testDbProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:mariadb://localhost:3306/ysk_asan_test");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "false");
    }
}
