package com.community.idle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.community.idle.constants.UserConstants;
import com.community.idle.dto.UserCancelDTO;
import com.community.idle.entity.User;
import com.community.idle.exception.BusinessException;
import com.community.idle.mapper.UserMapper;
import com.community.idle.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class UserCancelService {

    private final UserMapper userMapper;
    private final RedisUtil redisUtil;

    public UserCancelService(UserMapper userMapper, RedisUtil redisUtil) {
        this.userMapper = userMapper;
        this.redisUtil = redisUtil;
    }

    @Transactional(rollbackFor = Exception.class)
    public void applyCancel(Long userId, UserCancelDTO dto) {
        String lockKey = UserConstants.USER_CANCEL_LOCK_PREFIX + userId;
        Boolean locked = redisUtil.setString(lockKey, "1",
                UserConstants.USER_CANCEL_LOCK_EXPIRE, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            throw new BusinessException("操作过于频繁，请稍后再试");
        }

        try {
            User user = userMapper.selectById(userId);
            if (user == null) {
                throw new BusinessException("用户不存在");
            }

            if (UserConstants.STATUS_CANCELING.equals(user.getStatus())) {
                throw new BusinessException("账号已在注销申请中，无需重复申请");
            }

            if (UserConstants.STATUS_CANCELED.equals(user.getStatus())) {
                throw new BusinessException("账号已注销");
            }

            if (!UserConstants.STATUS_NORMAL.equals(user.getStatus())) {
                throw new BusinessException("账号状态异常，无法申请注销");
            }

            int updated = userMapper.update(null,
                    new LambdaUpdateWrapper<User>()
                            .eq(User::getId, userId)
                            .eq(User::getStatus, UserConstants.STATUS_NORMAL)
                            .set(User::getStatus, UserConstants.STATUS_CANCELING)
                            .set(User::getCancelApplyTime, LocalDateTime.now())
                            .set(User::getCancelReason, dto != null ? dto.getReason() : null)
                            .set(User::getUpdateTime, LocalDateTime.now())
            );

            if (updated == 0) {
                throw new BusinessException("注销申请提交失败，请重试");
            }

            log.info("用户注销申请提交成功, userId: {}, reason: {}", userId,
                    dto != null ? dto.getReason() : "无");

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("用户注销申请异常, userId: {}", userId, e);
            throw new BusinessException("注销申请提交失败，请稍后再试");
        } finally {
            redisUtil.del(lockKey);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelApply(Long userId) {
        String lockKey = UserConstants.USER_CANCEL_LOCK_PREFIX + userId;
        Boolean locked = redisUtil.setString(lockKey, "1",
                UserConstants.USER_CANCEL_LOCK_EXPIRE, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            throw new BusinessException("操作过于频繁，请稍后再试");
        }

        try {
            User user = userMapper.selectById(userId);
            if (user == null) {
                throw new BusinessException("用户不存在");
            }

            if (!UserConstants.STATUS_CANCELING.equals(user.getStatus())) {
                throw new BusinessException("账号未在注销申请中");
            }

            int updated = userMapper.update(null,
                    new LambdaUpdateWrapper<User>()
                            .eq(User::getId, userId)
                            .eq(User::getStatus, UserConstants.STATUS_CANCELING)
                            .set(User::getStatus, UserConstants.STATUS_NORMAL)
                            .set(User::getCancelApplyTime, null)
                            .set(User::getCancelReason, null)
                            .set(User::getUpdateTime, LocalDateTime.now())
            );

            if (updated == 0) {
                throw new BusinessException("取消注销申请失败，请重试");
            }

            log.info("用户取消注销申请成功, userId: {}", userId);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("取消注销申请异常, userId: {}", userId, e);
            throw new BusinessException("取消注销申请失败，请稍后再试");
        } finally {
            redisUtil.del(lockKey);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void processExpiredCancelUsers() {
        LocalDateTime expireTime = LocalDateTime.now().minusDays(UserConstants.CANCEL_PROCESS_DAYS);

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<User>()
                .eq(User::getStatus, UserConstants.STATUS_CANCELING)
                .le(User::getCancelApplyTime, expireTime)
                .last("LIMIT 100");

        java.util.List<User> users = userMapper.selectList(queryWrapper);

        if (users.isEmpty()) {
            return;
        }

        log.info("开始处理过期注销用户, 待处理数量: {}", users.size());

        int successCount = 0;
        int failCount = 0;

        for (User user : users) {
            try {
                anonymizeUser(user);
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("处理注销用户数据失败, userId: {}", user.getId(), e);
            }
        }

        log.info("处理过期注销用户完成, 成功: {}, 失败: {}", successCount, failCount);
    }

    private void anonymizeUser(User user) {
        String randomSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        int updated = userMapper.update(null,
                new LambdaUpdateWrapper<User>()
                        .eq(User::getId, user.getId())
                        .eq(User::getStatus, UserConstants.STATUS_CANCELING)
                        .set(User::getStatus, UserConstants.STATUS_CANCELED)
                        .set(User::getNickname, "已注销用户_" + randomSuffix)
                        .set(User::getAvatar, null)
                        .set(User::getPhone, null)
                        .set(User::getOpenid, null)
                        .set(User::getSessionKey, null)
                        .set(User::getGender, null)
                        .set(User::getCancelProcessTime, LocalDateTime.now())
                        .set(User::getUpdateTime, LocalDateTime.now())
        );

        if (updated > 0) {
            log.info("用户数据匿名化完成, userId: {}", user.getId());
        }
    }
}
