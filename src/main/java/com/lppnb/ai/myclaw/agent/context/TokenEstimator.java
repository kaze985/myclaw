package com.lppnb.ai.myclaw.agent.context;

import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

/**
 * 轻量 token 估算器：按字符数 × 系数近似估算 token 消耗。
 *
 * <p>不引入精确 tokenizer（需额外依赖且与模型版本强耦合）；
 * 系数保守取值（qwen 中文约 0.5-0.7 token/字），估算偏大以保证压缩早于真实超窗触发。</p>
 */
@Component
public class TokenEstimator {

    private final AgentContextProperties properties;

    public TokenEstimator(AgentContextProperties properties) {
        this.properties = properties;
    }

    /**
     * 估算一段文本的 token 数。
     *
     * @param text 文本（可为 null/空）
     * @return 估算 token 数（向上取整，最小 0）
     */
    public long estimate(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return (long) Math.ceil(text.length() * properties.getTokenEstimateRatio());
    }

    /**
     * 估算一条消息的 token 数。
     */
    public long estimate(Message message) {
        if (message == null || message.getText() == null) {
            return 0;
        }
        return estimate(message.getText());
    }
}
