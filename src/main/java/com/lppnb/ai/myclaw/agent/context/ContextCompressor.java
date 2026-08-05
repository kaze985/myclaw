package com.lppnb.ai.myclaw.agent.context;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 上下文压缩器：将一段早期对话消息交给模型压缩为要点摘要。
 *
 * <p>摘要用于替换被压缩的原始消息段，保留用户目标、已完成/未完成事项等关键信息，
 * 避免滑窗式丢弃导致 Agent 丢失任务初衷。</p>
 */
@Slf4j
@Component
public class ContextCompressor {

    /** 摘要长度上限（字符），防止摘要本身占用过多预算。 */
    private static final int SUMMARY_MAX_CHARS = 2000;

    private final ChatClient chatClient;

    public ContextCompressor(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * 将一段对话消息压缩为摘要文本。
     *
     * @param segment 被压缩的早期对话段（按完整用户回合切割，工具配对完整）
     * @return 摘要文本；压缩失败时返回 {@code null}（由调用方决定降级策略）
     */
    public String compress(List<Message> segment) {
        if (segment == null || segment.isEmpty()) {
            return "";
        }
        String transcript = segment.stream()
                .map(m -> roleOf(m) + ": " + safeText(m.getText()))
                .collect(Collectors.joining("\n"));
        String prompt = """
                以下是 AI 助手与用户的早期对话记录。请将其压缩为一份不超过 %d 个字符的
                中文要点摘要，供助手在新一轮推理中回顾。必须保留：
                1. 用户的原始目标与需求；
                2. 已完成的关键步骤与结论；
                3. 尚未完成/待办事项；
                4. 重要的背景事实与用户偏好。
                只输出摘要正文，不要任何前言或解释。
                
                对话记录：
                %s""".formatted(SUMMARY_MAX_CHARS, transcript);

        try {
            String summary = chatClient.prompt(prompt).call().content();
            if (summary == null || summary.isBlank()) {
                log.warn("Context compression returned empty summary.");
                return null;
            }
            if (summary.length() > SUMMARY_MAX_CHARS * 2) {
                summary = summary.substring(0, SUMMARY_MAX_CHARS * 2);
            }
            log.info("上下文压缩完成：{} 条消息 → 摘要 {} 字符", segment.size(), summary.length());
            return summary;
        } catch (Exception e) {
            log.error("上下文压缩失败，本次跳过压缩：{}", e.getMessage(), e);
            return null;
        }
    }

    private String roleOf(Message message) {
        return message.getMessageType().getValue();
    }

    private String safeText(String text) {
        return text == null ? "" : text;
    }
}
