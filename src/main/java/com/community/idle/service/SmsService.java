package com.community.idle.service;

import com.community.idle.config.SmsProperties;
import com.community.idle.exception.BusinessException;
import com.community.idle.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SmsService {

    private static final String SMS_CODE_PREFIX = "sms:code:";
    private static final String SMS_SEND_COUNT_PREFIX = "sms:count:";
    private static final String SMS_SEND_LOCK_PREFIX = "sms:lock:";

    private final SmsProperties smsProperties;
    private final RedisUtil redisUtil;
    private final Random random = new Random();

    public SmsService(SmsProperties smsProperties, RedisUtil redisUtil) {
        this.smsProperties = smsProperties;
        this.redisUtil = redisUtil;
    }

    public void sendSmsCode(String phone, String scene) {
        String lockKey = SMS_SEND_LOCK_PREFIX + phone;
        Boolean locked = redisUtil.setString(lockKey, "1", smsProperties.getSendIntervalSeconds(), TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            throw new BusinessException("发送验证码过于频繁，请稍后再试");
        }

        try {
            String countKey = SMS_SEND_COUNT_PREFIX + phone + ":" + scene;
            String countStr = redisUtil.getString(countKey);
            int count = countStr == null ? 0 : Integer.parseInt(countStr);
            if (count >= smsProperties.getMaxSendCountPerDay()) {
                throw new BusinessException("今日发送次数已达上限，请明日再试");
            }

            String code = generateCode();

            boolean sendSuccess = doSendSms(phone, code, smsProperties.getTemplateCodeChangePhone());
            if (!sendSuccess) {
                redisUtil.del(lockKey);
                throw new BusinessException("短信发送失败，请稍后再试");
            }

            String codeKey = SMS_CODE_PREFIX + scene + ":" + phone;
            redisUtil.setString(codeKey, code, smsProperties.getExpireSeconds(), TimeUnit.SECONDS);

            redisUtil.setString(countKey, String.valueOf(count + 1), getSecondsToTomorrow(), TimeUnit.SECONDS);

            log.info("短信验证码发送成功, phone: {}, scene: {}, code: {}", phone, scene, code);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("短信验证码发送异常, phone: {}, scene: {}", phone, scene, e);
            redisUtil.del(lockKey);
            throw new BusinessException("短信发送失败，请稍后再试");
        }
    }

    public boolean verifySmsCode(String phone, String scene, String code) {
        if (code == null || code.isEmpty()) {
            return false;
        }
        String codeKey = SMS_CODE_PREFIX + scene + ":" + phone;
        String storedCode = redisUtil.getString(codeKey);
        boolean result = code.equals(storedCode);
        if (result) {
            redisUtil.del(codeKey);
        }
        return result;
    }

    public void invalidateSmsCode(String phone, String scene) {
        String codeKey = SMS_CODE_PREFIX + scene + ":" + phone;
        redisUtil.del(codeKey);
    }

    private String generateCode() {
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    private boolean doSendSms(String phone, String code, String templateCode) {
        log.info("【模拟短信SDK调用】发送短信到: {}, 模板: {}, 参数: {}", phone, templateCode, code);
        return true;
    }

    private long getSecondsToTomorrow() {
        long now = System.currentTimeMillis();
        long tomorrow = now / (1000 * 60 * 60 * 24) * (1000 * 60 * 60 * 24) + (1000 * 60 * 60 * 24);
        return (tomorrow - now) / 1000;
    }
}
