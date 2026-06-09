package com.ysk.contact.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.ysk.contact.entity.Contact;
import com.ysk.contact.support.IntegrationTest;

class ContactRepositoryTest extends IntegrationTest {

    @Autowired
    ContactRepository contactRepository;

    private Contact save(String department, String name, String email) {
        return contactRepository.save(
                Contact.builder().department(department).name(name).email(email).build());
    }

    @Test
    void savesAndFindsById_withTimestamps() {
        Contact saved = save("영업", "홍길동", "hong@ex.com");

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(contactRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void search_noFilter_returnsAll() {
        save("영업", "홍길동", "hong@ex.com");
        save("개발", "김철수", "kim@ex.com");

        assertThat(contactRepository.search(null, null)).hasSize(2);
    }

    @Test
    void search_byDepartment_filtersExact() {
        save("영업", "홍길동", "hong@ex.com");
        save("개발", "김철수", "kim@ex.com");

        List<Contact> result = contactRepository.search("영업", null);

        assertThat(result).extracting(Contact::getDepartment).containsExactly("영업");
    }

    @Test
    void search_byQuery_matchesNameCaseInsensitive() {
        save("영업", "Alice", "alice@ex.com");
        save("개발", "Bob", "bob@example.com");

        assertThat(contactRepository.search(null, "alice")).extracting(Contact::getName)
                .containsExactly("Alice");
        assertThat(contactRepository.search(null, "ALICE")).extracting(Contact::getName)
                .containsExactly("Alice");
    }

    @Test
    void search_byQuery_matchesEmail() {
        save("영업", "Alice", "alice@ex.com");
        save("개발", "Bob", "bob@example.com");

        assertThat(contactRepository.search(null, "example.com")).extracting(Contact::getName)
                .containsExactly("Bob");
    }

    /**
     * 부서 필터와 검색어 OR 그룹이 올바르게 AND 결합되는지 검증한다(JPQL 괄호 가드).
     * 동일 이름이 두 부서에 존재할 때 department=영업 + q=공통이름 이면 영업 행만 나와야 한다.
     */
    @Test
    void search_departmentAndQuery_doesNotLeakAcrossDepartments() {
        save("영업", "공통이름", "sales@ex.com");
        save("개발", "공통이름", "dev@ex.com");

        List<Contact> result = contactRepository.search("영업", "공통이름");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("sales@ex.com");
    }

    @Test
    void findDistinctDepartments_returnsSortedDistinct() {
        save("영업", "a", "a@ex.com");
        save("영업", "b", "b@ex.com");
        save("개발", "c", "c@ex.com");

        List<String> departments = contactRepository.findDistinctDepartments();

        // 중복 제거 + 가나다 정렬(개발 < 영업)
        assertThat(departments).containsExactly("개발", "영업");
    }

    @Test
    void findDistinctDepartments_emptyTable_returnsEmpty() {
        assertThat(contactRepository.findDistinctDepartments()).isEmpty();
    }
}
