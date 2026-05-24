package com.community.idle.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "sms")
public class SmsProperties {

    private String accessKeyId;

    private String accessKeySecret;

    private String signName;

    private String templateCodeChangePhone;

    private Long expireSeconds = 300L;

    private Integer sendIntervalSeconds = 60;

    private Integer maxSendCountPerDay = 10;
}
