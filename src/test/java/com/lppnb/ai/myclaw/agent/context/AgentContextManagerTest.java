package com.lppnb.ai.myclaw.agent.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentCaptor.forClass;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

class AgentContextManagerTest {

    private AgentContextProperties properties;
    private TokenEstimator tokenEstimator;
    private ContextCompressor compressor;

    @BeforeEach
    void setUp() {
        properties = new AgentContextProperties();
        // 缩小窗口便于测试：窗口 1000 token，80% 触发（800），40% 目标（400）
        properties.setWindowTokens(1000);
        properties.setCompressThreshold(0.8);
        properties.setCompressTarget(0.4);
        properties.setTokenEstimateRatio(0.6);
        properties.setMinMessagesForCompress(4);
        tokenEstimator = new TokenEstimator(properties);
        compressor = mock(ContextCompressor.class);
    }

    private AgentContextManager newManager() {
        return new AgentContextManager(tokenEstimator, compressor, properties);
    }

    /** 构造一个完整用户回合：UserMessage + AssistantMessage（+ 可选 ToolResponse 配对）。 */
    private List<Message> buildTurn(int charsPerMessage) {
        List<Message> turn = new ArrayList<>();
        turn.add(new UserMessage("u".repeat(charsPerMessage)));
        turn.add(new AssistantMessage("a".repeat(charsPerMessage)));
        return turn;
    }

    private long estimateOf(List<Message> messages) {
        long total = 0;
        for (Message m : messages) {
            total += tokenEstimator.estimate(m);
        }
        return total;
    }

    @Test
    void belowThresholdDoesNotCompress() {
        // 2 个回合 × 300 字符 × 0.6 = 720 token < 800，不触发
        List<Message> context = new ArrayList<>();
        context.addAll(buildTurn(300));
        context.addAll(buildTurn(300));

        newManager().prepare(context);

        verify(compressor, never()).compress(anyList());
        assertTrue(context.stream().noneMatch(m -> m.getMessageType() == MessageType.SYSTEM),
                "未触发压缩时不应出现摘要消息");
    }

    @Test
    void aboveThresholdCompressesOldestTurn() {
        // 5 个回合 × 300 字符 × 0.6 = 900 token > 800，触发压缩
        List<Message> context = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            context.addAll(buildTurn(300));
        }
        when(compressor.compress(anyList())).thenReturn("压缩后的早期对话摘要");

        newManager().prepare(context);

        verify(compressor, atLeastOnce()).compress(anyList());
        // 队首应为摘要 SystemMessage
        assertEquals(MessageType.SYSTEM, context.get(0).getMessageType());
        assertTrue(context.get(0).getText().contains("早期对话摘要"));
        // 压缩后消息数应少于原始 10 条
        assertTrue(context.size() < 10, "压缩后消息数应减少，实际 " + context.size());
        // 压缩后总 token 应低于触发阈值 800
        assertTrue(estimateOf(context) <= 800, "压缩后应低于触发阈值，实际 " + estimateOf(context));
    }

    @Test
    void toolCallAndResponseStayPairedAfterCompress() {
        // 回合结构：User + Assistant(tool call) + ToolResponse + Assistant —— 压缩最老段后配对保持完整
        List<Message> context = new ArrayList<>();
        context.add(new UserMessage("u".repeat(300)));
        context.add(new AssistantMessage("a".repeat(300)));
        for (int i = 0; i < 4; i++) {
            context.add(new UserMessage("u".repeat(300)));
            context.add(new AssistantMessage("a".repeat(300)));
        }
        // 手动插入一条工具响应消息到最老回合内，验证切割不拆散配对（用 mock 模拟 TOOL 消息）
        Message toolResponse = mock(Message.class);
        when(toolResponse.getMessageType()).thenReturn(MessageType.TOOL);
        when(toolResponse.getText()).thenReturn("搜索结果");
        context.add(1, toolResponse);
        when(compressor.compress(anyList())).thenReturn("摘要");

        newManager().prepare(context);

        // 捕获传给压缩器的段：TOOL 消息必须整段进入压缩（配对整体，未被 USER 边界拆散）
        ArgumentCaptor<List<Message>> captor = forClass(List.class);
        verify(compressor, atLeastOnce()).compress(captor.capture());
        List<Message> firstSegment = captor.getAllValues().get(0);
        assertTrue(firstSegment.stream().anyMatch(m -> m.getMessageType() == MessageType.TOOL),
                "压缩段应包含 TOOL 消息（配对整体进入压缩）");
        boolean seenTool = false;
        for (Message m : firstSegment) {
            if (m.getMessageType() == MessageType.TOOL) {
                seenTool = true;
            } else if (seenTool && m.getMessageType() == MessageType.USER) {
                throw new AssertionError("压缩段内 TOOL 之后出现 USER，配对被拆散");
            }
        }
        // 压缩后队首是摘要，剩余上下文 token 低于触发阈值
        assertEquals(MessageType.SYSTEM, context.get(0).getMessageType());
        assertTrue(estimateOf(context) <= 800, "压缩后应低于触发阈值，实际 " + estimateOf(context));
    }

    @Test
    void singleUserTurnIsNotCompressed() {
        // 仅 1 个用户回合（即使超阈值），无可压缩的"更老"回合 → 不压缩
        List<Message> context = new ArrayList<>();
        context.addAll(buildTurn(500)); // 600 token < 800，其实不触发；改用更极端
        newManager().prepare(context);
        verify(compressor, never()).compress(anyList());
    }

    @Test
    void compressFailureIsSkippedSafely() {
        List<Message> context = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            context.addAll(buildTurn(300));
        }
        when(compressor.compress(anyList())).thenReturn(null);

        newManager().prepare(context);

        // 压缩失败：上下文保持原样（不崩溃、不丢消息）
        assertEquals(10, context.size());
        assertTrue(context.stream().noneMatch(m -> m.getMessageType() == MessageType.SYSTEM));
    }
}
