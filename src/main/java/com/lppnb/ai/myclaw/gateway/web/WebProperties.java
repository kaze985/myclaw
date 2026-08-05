package com.lppnb.ai.myclaw.gateway.web;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Web 通道配置属性，对应 application.yml 中 web 前缀的配置项。
 */
@Data
@ConfigurationProperties(prefix = "web")
public class WebProperties {

    /** 是否启用 Web 通道，默认关闭 */
    private boolean enabled = false;

    /** 访问密码（通过环境变量 WEB_ACCESS_PASSWORD 注入） */
    private String accessPassword;
}
