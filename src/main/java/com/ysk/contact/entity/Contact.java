package com.ysk.contact.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "contact")
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
