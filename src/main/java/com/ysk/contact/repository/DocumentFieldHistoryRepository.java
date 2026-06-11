package com.ysk.contact.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ysk.contact.entity.DocumentFieldHistory;

public interface DocumentFieldHistoryRepository extends JpaRepository<DocumentFieldHistory, Long> {

    Optional<DocumentFieldHistory> findByTemplateIdAndFieldKeyAndValue(
            String templateId, String fieldKey, String value);

    /** 최근 사용순(동률은 최신 id 우선). 서비스에서 필드별 상위 N 으로 그룹핑한다. */
    List<DocumentFieldHistory> findTop300ByTemplateIdOrderByLastUsedAtDescIdDesc(String templateId);
}
