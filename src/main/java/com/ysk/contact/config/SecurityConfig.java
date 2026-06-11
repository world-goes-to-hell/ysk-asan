package com.ysk.contact.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.server.CookieSameSiteSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final int REMEMBER_ME_VALIDITY_SECONDS = 14 * 24 * 60 * 60; // 14일

    // 토큰 서명 키(고정이어야 서버 재시작에도 쿠키 재인증 가능). 운영은 환경변수 필수.
    @Value("${remember-me.key}")
    private String rememberMeKey;

    // Secure 쿠키 여부: 리버스 프록시 뒤라 request.isSecure() 가 부정확 → profile 로 명시(prod=true).
    @Value("${remember-me.secure-cookie:false}")
    private boolean rememberMeSecureCookie;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * 서버가 내려보내는 모든 쿠키(JSESSIONID·remember-me·XSRF-TOKEN)에 SameSite=Lax 를 명시한다.
     * same-origin SPA 라 Lax 로 충분하며 CSRF 토큰과 함께 이중 방어가 된다.
     * (Strict 는 외부 링크로 진입 시 첫 요청에 쿠키가 빠져 로그아웃되는 UX 문제가 있어 제외)
     */
    @Bean
    public CookieSameSiteSupplier cookieSameSiteSupplier() {
        return CookieSameSiteSupplier.ofLax();
    }

    /**
     * 서명 토큰 기반 Remember-Me(DB 불필요). 커스텀 JSON 로그인이라 컨트롤러가 직접 loginSuccess 를
     * 호출하며, request 파라미터가 없으므로 alwaysRemember 로 파라미터 체크를 우회한다.
     * credential 이 인증 후 지워지므로 비밀번호 복구를 위해 UserDetailsService 가 필수.
     */
    @Bean
    public TokenBasedRememberMeServices rememberMeServices(UserDetailsService userDetailsService) {
        TokenBasedRememberMeServices services =
                new TokenBasedRememberMeServices(rememberMeKey, userDetailsService);
        services.setTokenValiditySeconds(REMEMBER_ME_VALIDITY_SECONDS);
        services.setAlwaysRemember(true);
        services.setUseSecureCookie(rememberMeSecureCookie);
        return services;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           TokenBasedRememberMeServices rememberMeServices) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers("/api/auth/register", "/api/auth/login")
                )
                // 보안 응답 헤더. X-Content-Type-Options(nosniff)는 Spring Security 기본 적용.
                // HSTS 는 secure 요청에만 나가므로 prod 의 forward-headers-strategy 로 https 를 인식시킨다.
                .headers(headers -> headers
                        // 클릭재킹 방지: 어떤 사이트에서도 iframe 임베드 불가.
                        .frameOptions(frame -> frame.deny())
                        // HTTPS 1년 강제 + 서브도메인 포함(프록시가 https 종단 → forward header 로 인식).
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31_536_000)))
                .authorizeHttpRequests(auth -> auth
                        // 비밀번호 변경은 로그인 필수 — /api/auth/** permitAll 보다 먼저(구체 규칙 우선).
                        .requestMatchers("/api/auth/password").authenticated()
                        .requestMatchers("/api/auth/**").permitAll()
                        // 공문 공개 열람/직인: 토큰+비밀번호로 보호(로그인 불필요). CSRF 는 그대로 적용.
                        // 발급(POST /api/documents)은 아래 /api/** authenticated 에 걸린다.
                        .requestMatchers("/api/documents/*/view", "/api/documents/*/seal").permitAll()
                        .requestMatchers("/assets/**", "/", "/index.html", "/favicon.ico").permitAll()
                        // 회원 관리는 ADMIN 전용(보다 구체적인 규칙이라 /api/** 앞에 선언).
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                // Remember-Me: 동일 services 인스턴스를 필터/프로바이더에 등록.
                // key 는 services 생성자 key 와 반드시 동일해야 프로바이더 검증이 맞아떨어진다.
                .rememberMe(rm -> rm
                        .rememberMeServices(rememberMeServices)
                        .key(rememberMeKey))
                // 미인증 접근 시 리다이렉트/403 대신 401 을 반환한다(프론트 apiFetch 세션 만료 처리용).
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                // CsrfFilter 뒤에서 토큰을 렌더링해 XSRF-TOKEN 쿠키 발급을 강제한다(SPA 첫 요청 403 방지).
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class);

        return http.build();
    }
}
