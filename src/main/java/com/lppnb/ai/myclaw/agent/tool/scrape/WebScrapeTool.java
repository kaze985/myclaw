package com.lppnb.ai.myclaw.agent.tool.scrape;

import cn.hutool.http.HttpUtil;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.LoadState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 动态网页抓取工具：优先使用 Hutool 静态抓取，JS 渲染页面自动回退到 Playwright Chromium。
 */
@Slf4j
@Component
public class WebScrapeTool {

    private static final int TIMEOUT_MS = 30_000;
    private static final int MIN_CONTENT_LENGTH = 100;

    @Tool(description = "Extract plain text content from a web page.")
    public String webScrape(
            @ToolParam(description = "The URL of the web page to scrape") String url) {
        // 静态优先
        try {
            String html = HttpUtil.get(url, TIMEOUT_MS);
            String text = extractText(html);
            if (isValidContent(text)) {
                return text;
            }
            log.debug("静态内容不足（{}字符），回退到 Playwright：{}", text.length(), url);
        } catch (Exception e) {
            log.debug("Hutool 静态抓取失败，回退到 Playwright：{} -> {}", url, e.getMessage());
        }

        // 动态回退
        return scrapeWithPlaywright(url);
    }

    private String extractText(String html) {
        if (!StringUtils.hasText(html)) {
            return "";
        }
        return html
                .replaceAll("(?is)<script[^>]*>.*?</script>", "")
                .replaceAll("(?is)<style[^>]*>.*?</style>", "")
                .replaceAll("<[^>]+>", "")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean isValidContent(String text) {
        return StringUtils.hasText(text) && text.length() >= MIN_CONTENT_LENGTH;
    }

    private String scrapeWithPlaywright(String url) {
        try (Playwright playwright = Playwright.create()) {
            Browser.NewContextOptions contextOptions = new Browser.NewContextOptions();
            try (Browser browser = playwright.chromium().launch(
                    new com.microsoft.playwright.BrowserType.LaunchOptions().setHeadless(true))) {
                try (BrowserContext context = browser.newContext()) {
                    try (Page page = context.newPage()) {
                        page.setDefaultTimeout(TIMEOUT_MS);
                        page.navigate(url);
                        page.waitForLoadState(LoadState.NETWORKIDLE);
                        String content = page.innerText("body");
                        return content.replaceAll("\\s+", " ").trim();
                    }
                }
            }
        } catch (TimeoutError e) {
            return "页面加载超时";
        } catch (Exception e) {
            log.error("Playwright 抓取失败：{}", url, e);
            return "网页抓取失败：" + e.getMessage();
        }
    }
}
