package com.lppnb.ai.myclaw.gateway;

import com.lppnb.ai.myclaw.agent.app.MyClaw;
import com.lppnb.ai.myclaw.gateway.channel.AgentMessageRouter;
import com.lppnb.ai.myclaw.gateway.channel.Channel;
import com.lppnb.ai.myclaw.gateway.channel.MessageRouter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Gateway 整体自动配置类，负责注册通用 Bean 并在应用启动后启动所有 Channel。
 */
@Slf4j
@Configuration
public class GatewayAutoConfiguration {

    @Bean
    public MessageRouter messageRouter(MyClaw agent) {
        return new AgentMessageRouter(agent);
    }

    @Bean
    public ApplicationRunner gatewayChannelStarter(List<Channel> channels) {
        return args -> {
            if (channels.isEmpty()) {
                log.info("No Gateway channels configured, skipping startup.");
                return;
            }
            log.info("Starting {} Gateway channel(s)...", channels.size());
            for (Channel channel : channels) {
                try {
                    channel.start();
                } catch (Exception e) {
                    log.error("Failed to start Gateway channel {}: {}", channel.getClass().getSimpleName(), e.getMessage(), e);
                }
            }
        };
    }
}
