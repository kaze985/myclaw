package com.lppnb.ai.myclaw.gateway.web;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Web 通道装配条件：仅当 {@code web.enabled=true} 且访问密码已配置时，
 * Web 通道的端点与组件才被注册（spec：web.enabled=false 或密码未配置时不提供端点）。
 */
public class WebChannelCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String enabled = context.getEnvironment().getProperty("web.enabled");
        String password = context.getEnvironment().getProperty("web.access-password");
        return "true".equalsIgnoreCase(enabled) && StringUtils.isNotBlank(password);
    }
}
