package com.community.idle.task;

import com.community.idle.service.UserCancelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ScheduledTask {

    private final UserCancelService userCancelService;

    public ScheduledTask(UserCancelService userCancelService) {
        this.userCancelService = userCancelService;
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void processExpiredCancelUsers() {
        log.info("开始执行定时任务：处理过期注销用户数据");
        try {
            userCancelService.processExpiredCancelUsers();
            log.info("定时任务执行完成：处理过期注销用户数据");
        } catch (Exception e) {
            log.error("定时任务执行异常：处理过期注销用户数据", e);
        }
    }
}
