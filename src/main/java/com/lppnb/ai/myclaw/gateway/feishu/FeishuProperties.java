package com.lppnb.ai.myclaw.gateway.feishu;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 飞书 Channel 配置属性，对应 application.yml 中 gateway.feishu 前缀的配置项。
 */
@Data
@ConfigurationProperties(prefix = "gateway.feishu")
public class FeishuProperties {

    /** 飞书应用 App ID */
    private String appId;

    /** 飞书应用 App Secret */
    private String appSecret;

    /** 是否启用飞书 Channel，默认启用 */
    private boolean enabled = true;
}
