package com.ysk.contact.config;

import java.io.IOException;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 매 요청에서 CsrfToken 을 렌더링(getToken 호출)해 XSRF-TOKEN 쿠키 발급을 강제한다.
 * <p>
 * Spring Security 6 의 CSRF 토큰은 지연 로딩(deferred)이라, 요청 중 토큰을 실제로 읽지 않으면
 * 쿠키가 발급되지 않는다. 로그인/회원가입은 CSRF 면제이고 {@code /api/auth/me} 도 토큰을 읽지
 * 않으므로, 이 필터가 없으면 SPA 의 첫 상태변경 요청(연락처 추가/수정/삭제)이 403 이 된다.
 * 이 필터는 부트스트랩 GET 응답에 쿠키를 실어 보내, 프론트가 {@code X-XSRF-TOKEN} 을 첨부할 수
 * 있게 한다.
 */
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            csrfToken.getToken(); // 토큰을 읽어 렌더링 → CookieCsrfTokenRepository 가 쿠키 발급
        }
        filterChain.doFilter(request, response);
    }
}
