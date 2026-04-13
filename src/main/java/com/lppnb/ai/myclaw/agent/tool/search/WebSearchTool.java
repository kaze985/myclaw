package com.lppnb.ai.myclaw.agent.tool.search;

import cn.hutool.http.HttpException;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 联网搜索工具：通过 Tavily REST API 实现网络检索，返回最多 5 条结构化结果。
 */
@Slf4j
@Component
public class WebSearchTool {

    @Value("${tools.tavily.api-key:}")
    private String apiKey;

    private static final String TAVILY_API_URL = "https://api.tavily.com/search";

    @Tool(description = "Search the internet and return relevant results.")
    public String webSearch(
            @ToolParam(description = "The search query") String query) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("Tavily API Key not configured. Please set tools.tavily.api-key in application.yml");
        }

        JSONObject requestBody = JSONUtil.createObj()
                .set("api_key", apiKey)
                .set("query", query)
                .set("max_results", 5);

        try {
            String responseBody = HttpRequest.post(TAVILY_API_URL)
                    .header("Content-Type", "application/json")
                    .body(requestBody.toString())
                    .timeout(10000)
                    .execute()
                    .body();

            JSONObject response = JSONUtil.parseObj(responseBody);
            JSONArray results = response.getJSONArray("results");

            if (results == null || results.isEmpty()) {
                return "未找到相关结果";
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(results.size(), 5); i++) {
                JSONObject item = results.getJSONObject(i);
                sb.append(i + 1).append(". **").append(item.getStr("title")).append("**\n");
                sb.append("   URL: ").append(item.getStr("url")).append("\n");
                sb.append("   ").append(item.getStr("content")).append("\n\n");
            }
            return sb.toString().trim();

        } catch (HttpException e) {
            String msg = e.getMessage();
            if (msg != null && (msg.contains("timeout") || msg.contains("timed out"))) {
                return "搜索超时，请稍后重试";
            }
            log.error("Tavily API 请求失败", e);
            return "搜索失败：" + msg;
        } catch (Exception e) {
            log.error("WebSearchTool 异常", e);
            return "搜索失败：" + e.getMessage();
        }
    }
}
