package com.lppnb.ai.myclaw.agent.context;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Agent 上下文管理器：在每次推理前检查上下文估算 token，
 * 达到窗口阈值时自动触发压缩（将最老的完整用户回合段压缩为摘要 SystemMessage 放回队首）。
 *
 * <p>切割按"用户消息边界"进行，保证 assistant 的 tool call 与 tool response 配对完整，
 * 避免产生孤儿工具消息导致模型报错或幻觉。</p>
 */
@Slf4j
@Component
public class AgentContextManager {

    private final TokenEstimator tokenEstimator;
    private final ContextCompressor contextCompressor;
    private final AgentContextProperties properties;

    public AgentContextManager(TokenEstimator tokenEstimator,
                               ContextCompressor contextCompressor,
                               AgentContextProperties properties) {
        this.tokenEstimator = tokenEstimator;
        this.contextCompressor = contextCompressor;
        this.properties = properties;
    }

    /**
     * 检查并（如需要）压缩上下文，使估算 token 低于触发阈值。
     * 压缩失败或无可压缩段时安全跳过，不阻塞推理。
     *
     * @param context 上下文消息列表（原地修改）
     */
    public void prepare(List<Message> context) {
        if (context == null || context.size() < properties.getMinMessagesForCompress()) {
            return;
        }
        long total = estimateTotal(context);
        long triggerAt = (long) (properties.getWindowTokens() * properties.getCompressThreshold());
        if (total <= triggerAt) {
            return;
        }
        log.info("上下文估算 {} token 超过触发阈值 {}，开始压缩（目标水位 {}）",
                total, triggerAt, (long) (properties.getWindowTokens() * properties.getCompressTarget()));
        compressDownToTarget(context);
    }

    private void compressDownToTarget(List<Message> context) {
        long targetAt = (long) (properties.getWindowTokens() * properties.getCompressTarget());
        int safety = 0;
        while (estimateTotal(context) > targetAt && ++safety <= 20) {
            Segment segment = findOldestCompressibleSegment(context);
            if (segment == null) {
                log.info("无可压缩的早期对话段（仅剩一个用户回合），停止压缩");
                return;
            }
            String summary = contextCompressor.compress(segment.messages());
            if (summary == null) {
                log.warn("压缩失败，本次放弃压缩，等待下一轮重试");
                return;
            }
            // 删除被压缩的原始消息，将摘要以 SystemMessage 放回队首
            context.subList(0, segment.endIndexExclusive()).clear();
            context.add(0, new SystemMessage("[早期对话摘要] " + summary));
            log.info("已压缩最老对话段（{} 条消息），当前估算 {} token",
                    segment.messages().size(), estimateTotal(context));
        }
    }

    /**
     * 定位最老的完整用户回合段：从第一个用户消息起，到下一个用户消息前（或列表末尾）。
     * 段起点之前的 SystemMessage（既有摘要）与段内的工具配对保持完整。
     */
    private Segment findOldestCompressibleSegment(List<Message> context) {
        int firstUser = -1;
        int secondUser = -1;
        for (int i = 0; i < context.size(); i++) {
            if (context.get(i).getMessageType() == MessageType.USER) {
                if (firstUser < 0) {
                    firstUser = i;
                } else {
                    secondUser = i;
                    break;
                }
            }
        }
        if (firstUser < 0) {
            return null; // 没有用户消息，无可压缩内容
        }
        // 若这是唯一的用户回合，压缩会清空对话内容——不可压缩
        if (secondUser < 0) {
            return null;
        }
        return new Segment(new ArrayList<>(context.subList(firstUser, secondUser)), secondUser);
    }

    private long estimateTotal(List<Message> context) {
        long total = 0;
        for (Message message : context) {
            total += tokenEstimator.estimate(message);
        }
        return total;
    }

    /** 待压缩的对话段：原始消息快照 + 段在列表中的结束索引（不含）。 */
    private record Segment(List<Message> messages, int endIndexExclusive) {
    }
}
