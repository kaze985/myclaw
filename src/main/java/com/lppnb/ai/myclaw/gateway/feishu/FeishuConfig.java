package com.lppnb.ai.myclaw.gateway.feishu;

import com.lppnb.ai.myclaw.gateway.channel.MessageRouter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 飞书 Channel 的 Spring 配置类，当 gateway.feishu.enabled=true 时注册相关 Bean。
 */
@Configuration
@EnableConfigurationProperties(FeishuProperties.class)
@ConditionalOnProperty(prefix = "gateway.feishu", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FeishuConfig {

    @Bean
    public FeishuEventHandler feishuEventHandler(MessageRouter messageRouter) {
        return new FeishuEventHandler(messageRouter);
    }

    @Bean
    public FeishuChannel feishuChannel(FeishuProperties feishuProperties, FeishuEventHandler feishuEventHandler) {
        return new FeishuChannel(feishuProperties, feishuEventHandler);
    }
}
