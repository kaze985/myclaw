package com.lppnb.ai.myclaw.agent.tool;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author kaze
 * @date 2026/4/10 14:46
 */
@Configuration
public class ToolRegistration {
    @Bean
    public ToolCallback[] allTools(){
        return ToolCallbacks.from(new TerminateTool());
    }
}
