package com.community.idle.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "canal")
public class CanalConfig {

    private boolean enabled = false;

    private String hostname = "127.0.0.1";

    private int port = 11111;

    private String destination = "example";

    private String username = "canal";

    private String password = "canal";

    private String database = "community_idle";

    private String table = "item";

    private int batchSize = 1000;

    private long sleepMillis = 1000;
}
