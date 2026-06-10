package com.ysk.contact.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ysk.contact.entity.User;
import com.ysk.contact.entity.UserRole;
import com.ysk.contact.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    /** 이 횟수만큼 연속으로 비밀번호를 틀리면 계정이 잠긴다(관리자 해제 필요). */
    public static final int MAX_LOGIN_FAILURES = 5;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User register(String username, String rawPassword) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("이미 사용 중인 사용자명입니다.");
        }
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(rawPassword))
                .role(UserRole.USER)
                .approved(false)    // 승인제: 관리자 승인 전 로그인 불가
                .build();
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    /**
     * 비밀번호 오류를 기록한다. 계정이 없으면 조용히 무시한다
     * (BadCredentials 는 미존재 계정도 동일하게 발생 — 존재 여부 노출 방지).
     *
     * @return 이번 실패로 계정이 잠겼으면 true
     */
    @Transactional
    public boolean recordLoginFailure(String username) {
        return userRepository.findByUsername(username)
                .map(user -> user.recordLoginFailure(MAX_LOGIN_FAILURES))
                .orElse(false);
    }

    /** 로그인 성공 시 실패 카운터 초기화(0이면 불필요한 쓰기 생략). */
    @Transactional
    public void resetLoginFailures(String username) {
        userRepository.findByUsername(username)
                .filter(user -> user.getFailedLoginAttempts() > 0)
                .ifPresent(User::resetLoginFailures);
    }

    /**
     * 본인 비밀번호 변경. 세션 탈취만으로는 변경할 수 없도록 현재 비밀번호를 확인한다.
     * 현재 비밀번호 검증은 AuthenticationManager 를 타지 않으므로 실패 카운트/잠금 대상이 아니다
     * (세션 보유자의 brute-force 는 사내툴 리스크로 수용).
     * 부수효과: remember-me 토큰 서명에 비밀번호가 포함되므로 변경 즉시 기존 쿠키가 전부 무효화된다.
     */
    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = findByUsername(username);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.");
        }
        user.changePassword(passwordEncoder.encode(newPassword));
    }
}
