package com.community.idle.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "wechat.subscribe")
public class WxSubMsgProperties {

    private String sendUrl = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send";

    private String tokenUrl = "https://api.weixin.qq.com/cgi-bin/token";

    private String appid;

    private String secret;

    private TemplateIds templates = new TemplateIds();

    @Data
    public static class TemplateIds {
        private String tradeSuccess = "";

        private String orderReminder = "";

        private String reviewResult = "";

        private String cancelNotice = "";

        private String itemOffShelf = "";
    }
}
