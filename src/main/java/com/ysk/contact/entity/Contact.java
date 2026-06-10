package com.ysk.contact.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 연락처(수신처). 전역 공유 데이터로 소유자 구분이 없으며, 이메일 중복을 허용한다.
 * 부서/이름/이메일은 수정 가능하므로 무분별한 @Setter 대신 의도된 {@link #update} 메서드만 노출한다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
// 복합 인덱스 (department, name): 검색 쿼리의 WHERE department= 와 ORDER BY department, name,
// 그리고 부서 distinct 조회(선행 컬럼 department)를 함께 가속한다.
// 이름/이메일 검색은 LIKE '%q%'(선행 와일드카드)라 B-tree 인덱스를 타지 못하므로 단독 인덱스를
// 두지 않는다. 데이터가 커지면 풀텍스트 인덱스(MATCH ... AGAINST)로 전환 검토.
// 주의: prod(ddl-auto=validate)는 인덱스를 검사/생성하지 않으므로, 운영 DB에는 최초 update 또는
// 수동(ALTER TABLE contact ADD INDEX ...) 적용이 필요하다(Flyway 도입 시 일원화 — Tier 3).
@Table(name = "contact", indexes = {
        @Index(name = "idx_contact_dept_name", columnList = "department, name")
})
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 부서/이름/이메일을 갱신한다. 영속성 컨텍스트가 관리하는 엔티티에서 호출하면
     * 트랜잭션 커밋 시 dirty checking 으로 반영된다(별도 save 불필요).
     */
    public void update(String department, String name, String email) {
        this.department = department;
        this.name = name;
        this.email = email;
    }
}
