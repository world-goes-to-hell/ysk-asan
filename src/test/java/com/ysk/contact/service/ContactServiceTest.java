package com.ysk.contact.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ysk.contact.dto.ContactRequest;
import com.ysk.contact.dto.ContactResponse;
import com.ysk.contact.entity.Contact;
import com.ysk.contact.exception.ContactNotFoundException;
import com.ysk.contact.repository.ContactRepository;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock
    ContactRepository contactRepository;

    @InjectMocks
    ContactService contactService;

    private Contact contact(Long id, String dept, String name, String email) {
        return Contact.builder().id(id).department(dept).name(name).email(email).build();
    }

    @Test
    void list_blankParams_normalizedToNull() {
        when(contactRepository.search(isNull(), isNull())).thenReturn(List.of());

        contactService.list("  ", "");

        verify(contactRepository).search(isNull(), isNull());
    }

    @Test
    void list_trimsValues() {
        when(contactRepository.search(any(), any())).thenReturn(List.of());

        contactService.list("  영업  ", "  kim  ");

        verify(contactRepository).search("영업", "kim");
    }

    @Test
    void create_savesAndReturnsDto() {
        ContactRequest req = new ContactRequest("영업", "홍길동", "hong@ex.com");
        when(contactRepository.save(any(Contact.class)))
                .thenReturn(contact(1L, "영업", "홍길동", "hong@ex.com"));

        ContactResponse res = contactService.create(req);

        assertThat(res.id()).isEqualTo(1L);
        assertThat(res.name()).isEqualTo("홍길동");
        verify(contactRepository).save(any(Contact.class));
    }

    @Test
    void update_existing_mutatesFields() {
        Contact existing = contact(1L, "영업", "홍길동", "hong@ex.com");
        when(contactRepository.findById(1L)).thenReturn(Optional.of(existing));

        ContactResponse res =
                contactService.update(1L, new ContactRequest("개발", "홍길순", "hong2@ex.com"));

        assertThat(existing.getDepartment()).isEqualTo("개발");
        assertThat(existing.getName()).isEqualTo("홍길순");
        assertThat(res.email()).isEqualTo("hong2@ex.com");
    }

    @Test
    void update_missing_throwsContactNotFound() {
        when(contactRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> contactService.update(99L, new ContactRequest("영업", "x", "x@ex.com")))
                .isInstanceOf(ContactNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void deleteByIds_allExist_returnsCount() {
        List<Long> ids = List.of(1L, 2L);
        when(contactRepository.findAllById(ids)).thenReturn(
                List.of(contact(1L, "a", "a", "a@ex.com"), contact(2L, "b", "b", "b@ex.com")));

        int deleted = contactService.deleteByIds(ids);

        assertThat(deleted).isEqualTo(2);
        verify(contactRepository).deleteAllInBatch(any());
    }

    @Test
    void deleteByIds_someMissing_returnsExistingCount() {
        List<Long> ids = List.of(1L, 2L, 3L);
        when(contactRepository.findAllById(ids))
                .thenReturn(List.of(contact(1L, "a", "a", "a@ex.com")));

        assertThat(contactService.deleteByIds(ids)).isEqualTo(1);
    }

    @Test
    void deleteByIds_empty_throwsWithoutHittingRepository() {
        assertThatThrownBy(() -> contactService.deleteByIds(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        verify(contactRepository, never()).findAllById(any());
    }

    @Test
    void deleteByIds_null_throws() {
        assertThatThrownBy(() -> contactService.deleteByIds(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void departments_delegatesToRepository() {
        when(contactRepository.findDistinctDepartments()).thenReturn(List.of("개발", "영업"));

        assertThat(contactService.departments()).containsExactly("개발", "영업");
    }
}
