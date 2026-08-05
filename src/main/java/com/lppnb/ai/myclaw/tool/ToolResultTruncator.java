package com.lppnb.ai.myclaw.tool;

/**
 * 工具结果截断器：防止单条超长工具结果（读大文件、整页抓取、终端长输出等）
 * 直接撑爆上下文预算，超限时截断并在末尾注明。
 */
public final class ToolResultTruncator {

    private ToolResultTruncator() {
    }

    /**
     * 截断超长文本。
     *
     * @param result   原始结果
     * @param maxChars 保留上限（字符数）；小于等于 0 表示不截断
     * @return 未超限返回原文；超限返回前 {@code maxChars} 字符 + 截断说明
     */
    public static String truncate(String result, int maxChars) {
        if (result == null || maxChars <= 0 || result.length() <= maxChars) {
            return result;
        }
        return result.substring(0, maxChars)
                + "\n\n…（内容过长已截断：共 " + result.length() + " 字符，仅显示前 " + maxChars + " 字符）";
    }
}
