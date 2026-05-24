package com.community.idle.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "watermark")
public class WatermarkProperties {

    private Boolean enabled = true;

    private String text = "社区闲置";

    private String logoPath;

    private Float opacity = 0.3f;

    private Integer fontSize = 30;

    private String color = "#FFFFFF";

    private String position = "BOTTOM_RIGHT";

    private Integer marginX = 20;

    private Integer marginY = 20;

    private String ffmpegPath = "ffmpeg";

    private String tempDir = "D:/community-idle-items/temp";
}
