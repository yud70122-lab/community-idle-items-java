package com.community.idle.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "item.sync")
public class ItemSyncConfig {

    private String type = "mq";

    public boolean isCanalEnabled() {
        return "canal".equalsIgnoreCase(type);
    }

    public boolean isMqEnabled() {
        return "mq".equalsIgnoreCase(type);
    }
}
