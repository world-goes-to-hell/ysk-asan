package com.ysk.contact.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ysk.contact.entity.Contact;

public interface ContactRepository extends JpaRepository<Contact, Long> {

    /**
     * 부서(정확 일치) + 검색어(이름/이메일 부분 일치, 대소문자 무시)로 연락처를 조회한다.
     * 두 조건 모두 선택적이며, null 이면 해당 조건을 적용하지 않는다.
     * <p>
     * 주의: 검색어 OR 그룹 전체를 괄호로 감싸 부서 필터와 AND 결합한다.
     * 괄호가 없으면 AND 가 OR 보다 우선 결합되어, 다른 부서의 이름/이메일 매치가
     * 부서 필터를 무시하고 새어 나온다.
     */
    @Query("""
            SELECT c FROM Contact c
            WHERE (:department IS NULL OR c.department = :department)
              AND (:q IS NULL
                   OR LOWER(c.name)  LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(c.email) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY c.department, c.name
            """)
    List<Contact> search(@Param("department") String department, @Param("q") String q);

    @Query("SELECT DISTINCT c.department FROM Contact c ORDER BY c.department")
    List<String> findDistinctDepartments();
}
