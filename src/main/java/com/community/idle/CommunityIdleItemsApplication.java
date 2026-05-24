package com.community.idle;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@MapperScan("com.community.idle.mapper")
public class CommunityIdleItemsApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommunityIdleItemsApplication.class, args);
    }
}
