package com.ysk.contact.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ysk.contact.entity.OfficialDocument;

public interface OfficialDocumentRepository extends JpaRepository<OfficialDocument, Long> {
    Optional<OfficialDocument> findByShareToken(String shareToken);
}
