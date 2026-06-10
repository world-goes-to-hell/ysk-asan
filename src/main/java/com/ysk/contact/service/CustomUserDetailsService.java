package com.ysk.contact.service;

import java.util.Collections;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ysk.contact.entity.User;
import com.ysk.contact.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // disabled/accountLocked 는 DaoAuthenticationProvider 가 비밀번호 검사 "이전"에 확인해
        // DisabledException(승인 대기)/LockedException(잠금) 으로 변환한다.
        // Remember-Me 쿠키 소비 시에도 AccountStatusUserDetailsChecker 가 동일하게 거부한다.
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .disabled(!user.isApproved())
                .accountLocked(user.isLocked())
                .build();
    }
}
