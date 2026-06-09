package com.ysk.contact.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.ysk.contact.dto.ContactRequest;
import com.ysk.contact.dto.ContactResponse;
import com.ysk.contact.entity.Contact;
import com.ysk.contact.exception.ContactNotFoundException;
import com.ysk.contact.repository.ContactRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactRepository contactRepository;

    /** 부서/검색어로 연락처 목록 조회. 빈 문자열·공백은 필터 미적용(null)으로 정규화한다. */
    @Transactional(readOnly = true)
    public List<ContactResponse> list(String department, String q) {
        return contactRepository.search(normalize(department), normalize(q)).stream()
                .map(ContactResponse::from)
                .toList();
    }

    /** 부서 탭용 distinct 부서 목록(정렬). */
    @Transactional(readOnly = true)
    public List<String> departments() {
        return contactRepository.findDistinctDepartments();
    }

    @Transactional
    public ContactResponse create(ContactRequest request) {
        Contact contact = Contact.builder()
                .department(request.department())
                .name(request.name())
                .email(request.email())
                .build();
        return ContactResponse.from(contactRepository.save(contact));
    }

    /** 연락처 수정. managed 엔티티를 변경하면 트랜잭션 커밋 시 dirty checking 으로 반영된다. */
    @Transactional
    public ContactResponse update(Long id, ContactRequest request) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new ContactNotFoundException(id));
        contact.update(request.department(), request.name(), request.email());
        return ContactResponse.from(contact);
    }

    /** 선택 항목 일괄 삭제. 존재하는 id 만 삭제하고 실제 삭제 개수를 반환한다. */
    @Transactional
    public int deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("삭제할 연락처를 선택하세요.");
        }
        List<Contact> found = contactRepository.findAllById(ids);
        contactRepository.deleteAllInBatch(found);
        return found.size();
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
