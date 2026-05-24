package com.community.idle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.idle.constants.CreditConstants;
import com.community.idle.constants.UserConstants;
import com.community.idle.dto.CreditRepairDTO;
import com.community.idle.entity.CreditRepairOrder;
import com.community.idle.entity.CreditViolation;
import com.community.idle.entity.User;
import com.community.idle.exception.BusinessException;
import com.community.idle.mapper.CreditRepairOrderMapper;
import com.community.idle.mapper.CreditViolationMapper;
import com.community.idle.mapper.UserMapper;
import com.community.idle.utils.RedisUtil;
import com.community.idle.vo.CreditRepairResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RepairService {

    private final CreditViolationMapper creditViolationMapper;
    private final CreditRepairOrderMapper creditRepairOrderMapper;
    private final UserMapper userMapper;
    private final RedisUtil redisUtil;
    private final CreditService creditService;

    public RepairService(CreditViolationMapper creditViolationMapper,
                         CreditRepairOrderMapper creditRepairOrderMapper,
                         UserMapper userMapper, RedisUtil redisUtil,
                         CreditService creditService) {
        this.creditViolationMapper = creditViolationMapper;
        this.creditRepairOrderMapper = creditRepairOrderMapper;
        this.userMapper = userMapper;
        this.redisUtil = redisUtil;
        this.creditService = creditService;
    }

    @Transactional(rollbackFor = Exception.class)
    public CreditRepairResultVO applyRepair(Long userId, CreditRepairDTO dto) {
        String lockKey = CreditConstants.CREDIT_LOCK_PREFIX + userId;
        Boolean locked = redisUtil.setString(lockKey, "1",
                CreditConstants.CREDIT_LOCK_EXPIRE, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            throw new BusinessException("操作过于频繁，请稍后再试");
        }

        try {
            User user = userMapper.selectById(userId);
            if (user == null) {
                throw new BusinessException("用户不存在");
            }
            if (!UserConstants.STATUS_NORMAL.equals(user.getStatus())) {
                throw new BusinessException("账号状态异常，无法进行此操作");
            }

            CreditViolation violation = creditViolationMapper.selectById(dto.getViolationId());
            if (violation == null) {
                throw new BusinessException("违约记录不存在");
            }
            if (!violation.getUserId().equals(userId)) {
                throw new BusinessException("无权操作此违约记录");
            }

            if (CreditConstants.VIOLATION_STATUS_REPAIRED.equals(violation.getStatus())) {
                throw new BusinessException("该违约记录已修复");
            }
            if (CreditConstants.VIOLATION_STATUS_PROCESSED.equals(violation.getStatus())) {
                throw new BusinessException("该违约记录已处理，无需修复");
            }
            if (!CreditConstants.VIOLATION_STATUS_PENDING.equals(violation.getStatus())) {
                throw new BusinessException("该违约记录状态不支持修复");
            }

            Long existingCount = creditRepairOrderMapper.selectCount(
                    new LambdaQueryWrapper<CreditRepairOrder>()
                            .eq(CreditRepairOrder::getUserId, userId)
                            .eq(CreditRepairOrder::getViolationId, dto.getViolationId())
                            .eq(CreditRepairOrder::getStatus, CreditConstants.REPAIR_STATUS_PENDING)
                            .eq(CreditRepairOrder::getDeleted, 0)
            );
            if (existingCount != null && existingCount > 0) {
                throw new BusinessException("该违约记录已有修复申请正在审核中，请勿重复提交");
            }

            String orderNo = generateOrderNo();

            CreditRepairOrder order = new CreditRepairOrder();
            order.setOrderNo(orderNo);
            order.setUserId(userId);
            order.setViolationId(dto.getViolationId());
            order.setRepairReason(dto.getRepairReason());
            order.setProveImages(dto.getProveImages());
            order.setStatus(CreditConstants.REPAIR_STATUS_PENDING);
            order.setCreateTime(LocalDateTime.now());
            order.setUpdateTime(LocalDateTime.now());
            creditRepairOrderMapper.insert(order);

            creditService.evictCreditCache(userId);

            log.info("信用修复申请提交成功, userId: {}, violationId: {}, orderNo: {}",
                    userId, dto.getViolationId(), orderNo);

            return CreditRepairResultVO.builder()
                    .orderNo(orderNo)
                    .status(CreditConstants.REPAIR_STATUS_PENDING)
                    .statusName("审核中")
                    .createTime(order.getCreateTime())
                    .build();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("信用修复申请异常, userId: {}", userId, e);
            throw new BusinessException("信用修复申请提交失败，请稍后再试");
        } finally {
            redisUtil.del(lockKey);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void auditRepair(Long orderId, boolean approved, String auditRemark, Long auditorId) {
        CreditRepairOrder order = creditRepairOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("修复工单不存在");
        }
        if (!CreditConstants.REPAIR_STATUS_PENDING.equals(order.getStatus())) {
            throw new BusinessException("该工单已审核，请勿重复操作");
        }

        if (approved) {
            order.setStatus(CreditConstants.REPAIR_STATUS_APPROVED);
            CreditViolation violation = creditViolationMapper.selectById(order.getViolationId());
            if (violation != null) {
                violation.setStatus(CreditConstants.VIOLATION_STATUS_REPAIRED);
                violation.setProcessTime(LocalDateTime.now());
                violation.setProcessRemark("信用修复通过，工单编号：" + order.getOrderNo());
                violation.setUpdateTime(LocalDateTime.now());
                creditViolationMapper.updateById(violation);
            }
        } else {
            order.setStatus(CreditConstants.REPAIR_STATUS_REJECTED);
        }

        order.setAuditorId(auditorId);
        order.setAuditRemark(auditRemark);
        order.setAuditTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        creditRepairOrderMapper.updateById(order);

        creditService.evictCreditCache(order.getUserId());

        log.info("信用修复工单审核完成, orderId: {}, orderNo: {}, approved: {}",
                orderId, order.getOrderNo(), approved);
    }

    private String generateOrderNo() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "CR" + timestamp + random;
    }
}
