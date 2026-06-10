package com.ysk.contact.config;

import org.springframework.beans.factory.annotation.Value;
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
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/assets/**", "/", "/index.html", "/favicon.ico").permitAll()
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
