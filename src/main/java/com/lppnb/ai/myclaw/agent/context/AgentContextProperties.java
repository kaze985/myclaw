package com.lppnb.ai.myclaw.agent.context;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Agent 上下文管理配置。
 *
 * <p>窗口以估算 token 计（qwen3.8-max 窗口为 1M）；当上下文估算 token 达到
 * {@code windowTokens * compressThreshold} 时，触发一次上下文压缩，
 * 将最旧的完整对话段交给模型压成摘要，直到估算 token 降到
 * {@code windowTokens * compressTarget} 以下（留出余量，避免频繁触发）。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "agent.context")
public class AgentContextProperties {

    /** 模型上下文窗口（估算 token），默认 qwen3.8-max 的 1M。 */
    private long windowTokens = 1_000_000;

    /** 压缩触发阈值（占窗口比例），默认 80%。 */
    private double compressThreshold = 0.8;

    /** 压缩目标水位（占窗口比例），默认 40%。 */
    private double compressTarget = 0.4;

    /** 字符→token 估算系数（qwen 中文约 0.5-0.7 token/字，保守取 0.6）。 */
    private double tokenEstimateRatio = 0.6;

    /** 单条工具结果截断上限（字符数），超过则截断并注明。 */
    private int maxToolResultChars = 20_000;

    /** 技能正文截断上限（字符数），超过则截断并注明（技能正文通常远小于此值）。 */
    private int maxSkillBodyChars = 8_000;

    /** 技能目录最多展示条数，超出部分折叠为计数提示。 */
    private int maxCatalogEntries = 50;

    /** 触发压缩所需的最少消息条数（防止超短上下文被压缩）。 */
    private int minMessagesForCompress = 6;
}
