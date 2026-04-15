package com.lppnb.ai.myclaw.tool.search;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpException;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Component
public class WallpaperSearchTool {

    private static final String API_URL = "https://wallpaper.soutushenqi.com/v1/wallpaper/list";
    private static final String PRODUCT_ID = "52";
    private static final String VERSION_CODE = "29119";
    private static final String SECRET_KEY = "d9fd3ec394";

    /**
     * 生成符合前端算法的 timestamp 头部值
     */
    private String generateTimestamp(long timeOffsetMillis) {
        long nowMillis = System.currentTimeMillis() + timeOffsetMillis;
        long t = nowMillis / 1000; // 秒级时间戳
        int check = (int) ((t ^ 334) % 1000);
        String checkStr = String.format("%03d", check);
        return t + checkStr;
    }

    /**
     * 动态生成签名（与前端完全一致）
     */
    private String generateSign(Map<String, Object> params) {
        Map<String, Object> sorted = new TreeMap<>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            Object value = entry.getValue();
            if (value == null) continue;
            if (value instanceof String && StrUtil.isBlank((String) value)) continue;
            sorted.put(entry.getKey(), value);
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : sorted.entrySet()) {
            sb.append(entry.getKey())
                    .append("=")
                    .append(entry.getValue().toString().trim())
                    .append("&");
        }
        if (sb.length() > 0) sb.setLength(sb.length() - 1);
        sb.append("&key=").append(SECRET_KEY);
        String signStr = sb.toString();
        log.debug("待签名字符串: {}", signStr);
        return SecureUtil.md5(signStr).toUpperCase();
    }

    @Tool(description = """
            Search for wallpaper/image resources by keyword.
            Returns a list of matching images including their title, description, image URL, dimensions and tags.
            """)
    public String wallpaperSearch(
            @ToolParam(description = "The search keyword, e.g. '风景', '二次元', 'cat'") String keyword,
            @ToolParam(description = "Page number starting from 0, default is 0") int page) {
        try {
            // 构建参数 Map（TreeMap 自动字母排序）
            Map<String, Object> paramMap = new TreeMap<>();
            paramMap.put("page", page);
            paramMap.put("product_id", PRODUCT_ID);
            paramMap.put("searchMode", "ACCURATE_SEARCH");
            paramMap.put("search_word", keyword);
            paramMap.put("version_code", VERSION_CODE);

            // 计算签名
            String sign = generateSign(paramMap);
            paramMap.put("sign", sign);

            // 构造请求体（严格按字母顺序编码）
            StringBuilder postBody = new StringBuilder();
            for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
                String encodedKey = URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8);
                String encodedValue = URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8);
                postBody.append(encodedKey).append("=").append(encodedValue).append("&");
            }
            String body = postBody.substring(0, postBody.length() - 1);

            // 生成自定义 timestamp 头（偏移量默认为0）
            String timestamp = generateTimestamp(0);

            // 发送请求
            HttpResponse response = HttpRequest.post(API_URL)
                    .header("accept", "application/json, text/plain, */*")
                    .header("accept-language", "zh-CN,zh;q=0.9")
                    .header("cache-control", "no-cache")
                    .header("content-type", "application/x-www-form-urlencoded")
                    .header("origin", "https://www.soutushenqi.com")
                    .header("pragma", "no-cache")
                    .header("sec-ch-ua", "\"Chromium\";v=\"146\", \"Not-A.Brand\";v=\"24\", \"Google Chrome\";v=\"146\"")
                    .header("sec-ch-ua-mobile", "?0")
                    .header("sec-ch-ua-platform", "\"Windows\"")
                    .header("sec-fetch-dest", "empty")
                    .header("sec-fetch-mode", "cors")
                    .header("sec-fetch-site", "same-site")
                    .header("timestamp", timestamp)  // 使用算法生成的时间戳
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36")
                    .body(body)
                    .timeout(15000)
                    .execute();

            String responseBody = response.body();
            JSONObject json = JSONUtil.parseObj(responseBody);

            if (json.getInt("code", -1) != 200) {
                String errMsg = json.getStr("error_msg", "未知错误");
                log.warn("壁纸搜索接口返回异常：code={}, msg={}", json.getInt("code"), errMsg);
                return "搜索失败：" + errMsg;
            }

            JSONArray data = json.getJSONArray("data");
            if (data == null || data.isEmpty()) {
                return "未找到与「" + keyword + "」相关的图片";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("共找到 ").append(data.size()).append(" 张图片（第 ").append(page).append(" 页）：\n\n");

            for (int i = 0; i < data.size(); i++) {
                JSONObject item = data.getJSONObject(i);
                String detailInfo = item.getStr("detailInfo");
                String largeUrl = item.getStr("largeUrl");
                String thumbUrl = item.getStr("thumbUrl");
                int width = item.getInt("width", 0);
                int height = item.getInt("height", 0);
                int likeCount = item.getInt("likeCount", 0);
                String tagList = item.getStr("tagList");

                sb.append(i + 1).append(". ");
                if (StrUtil.isNotBlank(detailInfo)) {
                    sb.append(detailInfo.replaceAll("<[^>]+>", "").trim());
                } else {
                    sb.append("（无标题）");
                }
                sb.append("\n");
                sb.append("   尺寸：").append(width).append("×").append(height);
                sb.append("  点赞：").append(likeCount).append("\n");
                if (StrUtil.isNotBlank(tagList)) {
                    sb.append("   标签：").append(tagList).append("\n");
                }
                sb.append("   原图：").append(largeUrl).append("\n");
                sb.append("   缩略图：").append(thumbUrl).append("\n\n");
            }

            return sb.toString().trim();

        } catch (HttpException e) {
            String msg = e.getMessage();
            if (msg != null && (msg.contains("timeout") || msg.contains("timed out"))) {
                return "搜索超时，请稍后重试";
            }
            log.error("壁纸搜索 API 请求失败", e);
            return "搜索失败：" + msg;
        } catch (Exception e) {
            log.error("WallpaperSearchTool 异常", e);
            return "搜索失败：" + e.getMessage();
        }
    }
}