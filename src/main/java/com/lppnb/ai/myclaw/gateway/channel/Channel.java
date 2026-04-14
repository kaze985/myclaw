package com.lppnb.ai.myclaw.gateway.channel;

/**
 * 通道插件化接口，所有通讯平台集成均需实现该接口。
 */
public interface Channel {

    /** 启动通道，建立与平台的连接 */
    void start();

    /** 停止通道，优雅关闭连接并释放资源 */
    void stop();

    /** 向指定会话发送消息 */
    void send(GatewayMessage message);
}
