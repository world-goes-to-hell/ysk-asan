package com.ysk.contact.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ysk.contact.dto.AdminUserResponse;
import com.ysk.contact.entity.User;
import com.ysk.contact.entity.UserRole;
import com.ysk.contact.exception.UserNotFoundException;
import com.ysk.contact.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * 회원 관리(관리자 전용): 목록 조회·가입 승인·잠금 해제·권한 변경.
 * 엔드포인트 인가는 SecurityConfig 의 {@code /api/admin/** hasRole(ADMIN)} 이 담당한다.
 */
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<AdminUserResponse> list() {
        return userRepository.findAll(Sort.by(Sort.Direction.ASC, "createdAt", "id")).stream()
                .map(AdminUserResponse::from)
                .toList();
    }

    @Transactional
    public AdminUserResponse approve(Long id) {
        User user = find(id);
        user.approve();
        return AdminUserResponse.from(user);
    }

    @Transactional
    public AdminUserResponse unlock(Long id) {
        User user = find(id);
        user.unlock();
        return AdminUserResponse.from(user);
    }

    /**
     * 권한 변경. 자기 자신은 변경 불가 — 마지막 관리자가 스스로 강등해 관리자가 0명이 되는
     * 사고를 막는 최소 가드.
     */
    @Transactional
    public AdminUserResponse changeRole(Long id, UserRole role, String requesterUsername) {
        User user = find(id);
        if (user.getUsername().equals(requesterUsername)) {
            throw new IllegalArgumentException("자기 자신의 권한은 변경할 수 없습니다.");
        }
        user.changeRole(role);
        return AdminUserResponse.from(user);
    }

    private User find(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }
}
