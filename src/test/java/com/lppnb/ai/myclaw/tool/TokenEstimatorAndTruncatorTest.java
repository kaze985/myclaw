package com.lppnb.ai.myclaw.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.lppnb.ai.myclaw.agent.context.TokenEstimator;

class TokenEstimatorAndTruncatorTest {

    // ---- TokenEstimator ----

    @Test
    void estimateCountsCharsWithRatio() {
        TokenEstimator estimator = new TokenEstimator(new com.lppnb.ai.myclaw.agent.context.AgentContextProperties());
        assertEquals(0, estimator.estimate((String) null));
        assertEquals(0, estimator.estimate(""));
        // 100 字符 × 0.6 = 60
        assertEquals(60, estimator.estimate("a".repeat(100)));
        // 中文同样按字符估算
        assertEquals(60, estimator.estimate("你".repeat(100)));
    }

    // ---- ToolResultTruncator ----

    @Test
    void shortResultIsUntouched() {
        assertEquals("hello", ToolResultTruncator.truncate("hello", 20));
        assertNull(ToolResultTruncator.truncate(null, 20));
    }

    @Test
    void longResultIsTruncatedWithNotice() {
        String longText = "x".repeat(1000);
        String result = ToolResultTruncator.truncate(longText, 100);
        assertEquals(100, result.indexOf("\n\n…（内容过长已截断"));
        assertTrue(result.contains("共 1000 字符"), "截断说明应包含原始长度");
        assertTrue(result.contains("仅显示前 100 字符"), "截断说明应包含保留长度");
    }

    @Test
    void zeroOrNegativeMaxDisablesTruncation() {
        String text = "x".repeat(100);
        assertEquals(text, ToolResultTruncator.truncate(text, 0));
        assertEquals(text, ToolResultTruncator.truncate(text, -1));
    }
}
