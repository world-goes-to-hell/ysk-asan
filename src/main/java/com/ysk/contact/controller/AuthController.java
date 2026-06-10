package com.ysk.contact.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ysk.contact.dto.LoginRequest;
import com.ysk.contact.dto.RegisterRequest;
import com.ysk.contact.dto.UserResponse;
import com.ysk.contact.entity.User;
import com.ysk.contact.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String SPRING_SECURITY_CONTEXT_KEY = "SPRING_SECURITY_CONTEXT";

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    // TokenBasedRememberMeServices 는 RememberMeServices(loginSuccess) + LogoutHandler(logout) 를 모두 구현.
    private final TokenBasedRememberMeServices rememberMeServices;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request.username(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request,
                                   HttpServletRequest httpRequest,
                                   HttpServletResponse httpResponse) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            // 세션 고정 공격 방어: 로그인 전 (익명) 세션이 있으면 무효화 후 새 세션 발급
            HttpSession oldSession = httpRequest.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }
            HttpSession session = httpRequest.getSession(true);
            session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, context);

            // "로그인 상태 유지" 체크 시에만 remember-me 쿠키 발급(브라우저 종료·서버 재시작에도 유지).
            if (request.rememberMe()) {
                rememberMeServices.loginSuccess(httpRequest, httpResponse, authentication);
            }

            userService.resetLoginFailures(request.username());
            User user = userService.findByUsername(request.username());
            return ResponseEntity.ok(UserResponse.from(user));
        } catch (LockedException e) {
            // 잠금/승인대기는 비밀번호 검사 전에 발생(DaoAuthenticationProvider pre-check) → 실패 카운트 제외.
            return unauthorized("계정이 잠겨 있습니다. 관리자에게 잠금 해제를 요청하세요.");
        } catch (DisabledException e) {
            return unauthorized("관리자 승인 대기 중인 계정입니다. 승인 후 로그인할 수 있습니다.");
        } catch (BadCredentialsException e) {
            // 존재하지 않는 계정도 BadCredentials 로 동일 처리됨(존재 여부 비노출) — 카운트는 실계정만.
            boolean lockedNow = userService.recordLoginFailure(request.username());
            return unauthorized(lockedNow
                    ? "비밀번호 " + UserService.MAX_LOGIN_FAILURES + "회 오류로 계정이 잠겼습니다. 관리자에게 문의하세요."
                    : "사용자명 또는 비밀번호가 올바르지 않습니다.");
        } catch (AuthenticationException e) {
            return unauthorized("사용자명 또는 비밀번호가 올바르지 않습니다.");
        }
    }

    private ResponseEntity<Map<String, String>> unauthorized(String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", message));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        // remember-me 쿠키를 maxAge=0 으로 만료시킨다(auth 가 null 이어도 안전).
        rememberMeServices.logout(httpRequest, httpResponse, auth);
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userService.findByUsername(authentication.getName());
        return ResponseEntity.ok(UserResponse.from(user));
    }
}
