package com.lppnb.ai.myclaw.gateway.web;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 通道自动配置：仅在 {@code web.enabled=true} 且访问密码已配置时注册相关 Bean 与拦截器。
 */
@Configuration
@EnableConfigurationProperties(WebProperties.class)
@Conditional(WebChannelCondition.class)
public class WebChannelAutoConfiguration {

    @Bean
    public WebAuthInterceptor webAuthInterceptor(WebSessionRegistry sessionRegistry) {
        return new WebAuthInterceptor(sessionRegistry);
    }

    /** 拦截 Web 通道端点：聊天与产物下载；/auth/** 不拦截 */
    @Bean
    public WebMvcConfigurer webMvcConfigurer(WebAuthInterceptor webAuthInterceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(webAuthInterceptor)
                        .addPathPatterns("/chat/**", "/files/**");
            }
        };
    }
}
