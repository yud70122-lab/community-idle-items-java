package com.community.idle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.community.idle.constants.UserConstants;
import com.community.idle.dto.PhoneChangeDTO;
import com.community.idle.dto.PhoneChangeSendCodeDTO;
import com.community.idle.entity.User;
import com.community.idle.exception.BusinessException;
import com.community.idle.mapper.UserMapper;
import com.community.idle.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class PhoneChangeService {

    private final UserMapper userMapper;
    private final SmsService smsService;
    private final RedisUtil redisUtil;

    public PhoneChangeService(UserMapper userMapper, SmsService smsService, RedisUtil redisUtil) {
        this.userMapper = userMapper;
        this.smsService = smsService;
        this.redisUtil = redisUtil;
    }

    public void sendSmsCode(Long userId, PhoneChangeSendCodeDTO dto) {
        User user = getUserById(userId);

        String phone = dto.getPhone();
        String type = dto.getType();

        if ("old".equals(type)) {
            if (user.getPhone() == null) {
                throw new BusinessException("当前账号未绑定手机号，请先绑定");
            }
            if (!phone.equals(user.getPhone())) {
                throw new BusinessException("手机号与当前绑定手机号不一致");
            }
            smsService.sendSmsCode(phone, UserConstants.SMS_SCENE_CHANGE_OLD);
        } else if ("new".equals(type)) {
            if (phone.equals(user.getPhone())) {
                throw new BusinessException("新手机号不能与当前手机号相同");
            }
            User existingUser = userMapper.selectOne(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getPhone, phone)
                            .eq(User::getDeleted, 0)
            );
            if (existingUser != null) {
                throw new BusinessException("该手机号已被其他账号绑定");
            }
            smsService.sendSmsCode(phone, UserConstants.SMS_SCENE_CHANGE_NEW);
        } else {
            throw new BusinessException("验证码类型错误");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void changePhone(Long userId, PhoneChangeDTO dto) {
        String lockKey = UserConstants.PHONE_CHANGE_LOCK_PREFIX + userId;
        Boolean locked = redisUtil.setString(lockKey, "1",
                UserConstants.PHONE_CHANGE_LOCK_EXPIRE, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            throw new BusinessException("操作过于频繁，请稍后再试");
        }

        try {
            User user = getUserById(userId);

            if (user.getPhone() == null) {
                throw new BusinessException("当前账号未绑定手机号");
            }

            boolean oldCodeValid = smsService.verifySmsCode(
                    user.getPhone(),
                    UserConstants.SMS_SCENE_CHANGE_OLD,
                    dto.getOldCode()
            );
            if (!oldCodeValid) {
                throw new BusinessException("旧手机号验证码错误或已过期");
            }

            boolean newCodeValid = smsService.verifySmsCode(
                    dto.getNewPhone(),
                    UserConstants.SMS_SCENE_CHANGE_NEW,
                    dto.getNewCode()
            );
            if (!newCodeValid) {
                smsService.invalidateSmsCode(user.getPhone(), UserConstants.SMS_SCENE_CHANGE_OLD);
                throw new BusinessException("新手机号验证码错误或已过期");
            }

            User existingUser = userMapper.selectOne(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getPhone, dto.getNewPhone())
                            .eq(User::getDeleted, 0)
            );
            if (existingUser != null) {
                throw new BusinessException("该手机号已被其他账号绑定");
            }

            int updated = userMapper.update(null,
                    new LambdaUpdateWrapper<User>()
                            .eq(User::getId, userId)
                            .eq(User::getPhone, user.getPhone())
                            .set(User::getPhone, dto.getNewPhone())
                            .set(User::getUpdateTime, LocalDateTime.now())
            );

            if (updated == 0) {
                throw new BusinessException("手机号更新失败，请重试");
            }

            log.info("用户手机号换绑成功, userId: {}, oldPhone: {}, newPhone: {}",
                    userId, user.getPhone(), dto.getNewPhone());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("换绑手机号异常, userId: {}", userId, e);
            throw new BusinessException("换绑手机号失败，请稍后再试");
        } finally {
            redisUtil.del(lockKey);
        }
    }

    private User getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!UserConstants.STATUS_NORMAL.equals(user.getStatus())) {
            throw new BusinessException("账号状态异常，无法进行此操作");
        }
        return user;
    }
}
