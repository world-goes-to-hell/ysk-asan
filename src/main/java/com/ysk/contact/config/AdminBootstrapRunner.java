package com.ysk.contact.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ysk.contact.entity.UserRole;
import com.ysk.contact.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * 최초 관리자 부트스트랩. {@code APP_BOOTSTRAP_ADMIN} 에 지정된 계정을 기동 시
 * 승인 + ADMIN 승격 + 잠금 해제한다(멱등).
 *
 * <p>두 가지 문제를 해결한다:
 * <ul>
 *   <li>닭-달걀: 승인제 도입 시 "승인해 줄 첫 관리자"가 없음.</li>
 *   <li>break-glass: 관리자 전원이 잠기는 사고 시 재기동만으로 지정 계정 복구.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserRepository userRepository;

    @Value("${app.bootstrap-admin:}")
    private String bootstrapAdminUsername;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (bootstrapAdminUsername == null || bootstrapAdminUsername.isBlank()) {
            return;
        }
        userRepository.findByUsername(bootstrapAdminUsername).ifPresentOrElse(
                user -> {
                    // @Transactional 내 영속 엔티티 — dirty checking 으로 커밋 시 반영(save 불필요).
                    user.approve();
                    user.unlock();
                    user.changeRole(UserRole.ADMIN);
                    log.info("부트스트랩 관리자 적용 완료(승인·ADMIN·잠금해제): {}", bootstrapAdminUsername);
                },
                // 가입 전이면 스킵 — 가입 후 재기동하면 적용된다.
                () -> log.warn("부트스트랩 관리자 계정이 아직 존재하지 않습니다(가입 후 재기동 필요): {}",
                        bootstrapAdminUsername));
    }
}
