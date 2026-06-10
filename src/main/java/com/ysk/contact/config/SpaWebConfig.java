package com.ysk.contact.config;

import java.io.IOException;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * SPA 정적 리소스 서빙. React 빌드 산출물(classpath:/static/)을 제공하고,
 * 존재하지 않는 비-API 경로는 index.html 로 폴백해 클라이언트 사이드 라우팅(딥링크)을 지원한다.
 * API(/api/**)는 @RequestMapping 컨트롤러가 우선 매핑되므로 여기서 가로채지 않는다.
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        // API 요청은 폴백하지 않는다(컨트롤러 미스 시 정상 404 유지).
                        if (resourcePath.startsWith("api/")) {
                            return null;
                        }
                        // 그 외 매칭 실패 경로는 SPA 진입점으로 폴백(빌드 전이면 index.html 부재 → 404).
                        Resource index = new ClassPathResource("/static/index.html");
                        return index.exists() ? index : null;
                    }
                });
    }
}
