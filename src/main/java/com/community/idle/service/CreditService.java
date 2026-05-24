package com.community.idle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.idle.constants.CreditConstants;
import com.community.idle.entity.CreditNode;
import com.community.idle.entity.CreditViolation;
import com.community.idle.entity.OrderInfo;
import com.community.idle.entity.User;
import com.community.idle.entity.UserCredit;
import com.community.idle.exception.BusinessException;
import com.community.idle.mapper.CreditNodeMapper;
import com.community.idle.mapper.CreditViolationMapper;
import com.community.idle.mapper.OrderInfoMapper;
import com.community.idle.mapper.UserCreditMapper;
import com.community.idle.mapper.UserMapper;
import com.community.idle.utils.RedisUtil;
import com.community.idle.vo.CreditNodeVO;
import com.community.idle.vo.CreditTreeVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CreditService {

    private final UserCreditMapper userCreditMapper;
    private final CreditNodeMapper creditNodeMapper;
    private final UserMapper userMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final CreditViolationMapper creditViolationMapper;
    private final RedisUtil redisUtil;

    public CreditService(UserCreditMapper userCreditMapper, CreditNodeMapper creditNodeMapper,
                         UserMapper userMapper, OrderInfoMapper orderInfoMapper,
                         CreditViolationMapper creditViolationMapper, RedisUtil redisUtil) {
        this.userCreditMapper = userCreditMapper;
        this.creditNodeMapper = creditNodeMapper;
        this.userMapper = userMapper;
        this.orderInfoMapper = orderInfoMapper;
        this.creditViolationMapper = creditViolationMapper;
        this.redisUtil = redisUtil;
    }

    public CreditTreeVO getCreditTree(Long userId) {
        String cacheKey = CreditConstants.CREDIT_CACHE_PREFIX + userId;
        Object cached = redisUtil.get(cacheKey);
        if (cached instanceof CreditTreeVO) {
            return (CreditTreeVO) cached;
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        UserCredit userCredit = getUserCredit(userId);

        int pendingViolationCount = countPendingViolations(userId);

        LocalDateTime firstTradeTime = getFirstTradeTime(userId);
        int totalTradeCount = countTotalTrades(userId);
        int totalFulfillCount = countTotalFulfills(userId);
        int continuousDays = calculateContinuousFulfillDays(userId);
        int maxContinuousDays = getMaxContinuousDays(userId);

        List<CreditNode> nodes = creditNodeMapper.selectList(
                new LambdaQueryWrapper<CreditNode>()
                        .eq(CreditNode::getStatus, 1)
                        .orderByAsc(CreditNode::getSortOrder)
        );

        List<CreditNodeVO> nodeVOs = buildCreditNodeVOs(nodes, userCredit, user,
                firstTradeTime, totalTradeCount, continuousDays);

        CreditTreeVO result = CreditTreeVO.builder()
                .userId(userId)
                .creditScore(userCredit.getCreditScore())
                .creditLevel(userCredit.getCreditLevel())
                .creditLevelName(getCreditLevelName(userCredit.getCreditLevel()))
                .identityVerified(userCredit.getIdentityVerified() == 1)
                .phoneVerified(user.getPhone() != null && userCredit.getPhoneVerified() == 1)
                .firstTradeTime(firstTradeTime)
                .continuousFulfillDays(continuousDays)
                .maxContinuousFulfillDays(maxContinuousDays)
                .totalTradeCount(totalTradeCount)
                .totalFulfillCount(totalFulfillCount)
                .pendingViolationCount(pendingViolationCount)
                .nodes(nodeVOs)
                .build();

        redisUtil.set(cacheKey, result, CreditConstants.CREDIT_CACHE_EXPIRE);

        return result;
    }

    private List<CreditNodeVO> buildCreditNodeVOs(List<CreditNode> nodes, UserCredit userCredit,
                                                  User user, LocalDateTime firstTradeTime,
                                                  int totalTradeCount, int continuousDays) {
        if (nodes == null || nodes.isEmpty()) {
            return new ArrayList<>();
        }

        boolean hasUnlockedCurrent = false;
        List<CreditNode> sortedNodes = nodes.stream()
                .sorted(Comparator.comparingInt(CreditNode::getSortOrder))
                .collect(Collectors.toList());

        List<CreditNodeVO> result = new ArrayList<>();
        for (CreditNode node : sortedNodes) {
            CreditNodeVO vo = convertToNodeVO(node, userCredit, user,
                    firstTradeTime, totalTradeCount, continuousDays);

            if (vo.getStatus() == CreditConstants.NODE_STATUS_UNLOCKED) {
                hasUnlockedCurrent = true;
            } else if (vo.getStatus() == CreditConstants.NODE_STATUS_LOCKED && !hasUnlockedCurrent) {
                vo.setStatus(CreditConstants.NODE_STATUS_CURRENT);
                vo.setStatusName("进行中");
                hasUnlockedCurrent = true;
            }

            result.add(vo);
        }

        return result;
    }

    private CreditNodeVO convertToNodeVO(CreditNode node, UserCredit userCredit, User user,
                                         LocalDateTime firstTradeTime, int totalTradeCount,
                                         int continuousDays) {
        int currentValue = 0;
        int targetValue = 1;

        String nodeCode = node.getNodeCode();
        switch (nodeCode) {
            case "PHONE_VERIFIED":
                currentValue = (user.getPhone() != null && userCredit.getPhoneVerified() == 1) ? 1 : 0;
                targetValue = 1;
                break;
            case "IDENTITY_VERIFIED":
                currentValue = userCredit.getIdentityVerified() == 1 ? 1 : 0;
                targetValue = 1;
                break;
            case "FIRST_TRADE":
                currentValue = firstTradeTime != null ? 1 : 0;
                targetValue = 1;
                break;
            case "TRADE_COUNT_5":
                currentValue = Math.min(totalTradeCount, 5);
                targetValue = 5;
                break;
            case "TRADE_COUNT_10":
                currentValue = Math.min(totalTradeCount, 10);
                targetValue = 10;
                break;
            case "TRADE_COUNT_50":
                currentValue = Math.min(totalTradeCount, 50);
                targetValue = 50;
                break;
            case "CONTINUOUS_DAYS_7":
                currentValue = Math.min(continuousDays, 7);
                targetValue = 7;
                break;
            case "CONTINUOUS_DAYS_30":
                currentValue = Math.min(continuousDays, 30);
                targetValue = 30;
                break;
            case "CREDIT_SCORE_100":
                currentValue = Math.min(userCredit.getCreditScore(), 100);
                targetValue = 100;
                break;
            case "CREDIT_SCORE_500":
                currentValue = Math.min(userCredit.getCreditScore(), 500);
                targetValue = 500;
                break;
            default:
                currentValue = 0;
                targetValue = node.getRequiredScore() != null ? node.getRequiredScore() : 1;
                if (node.getRequiredDays() != null && node.getRequiredDays() > 0) {
                    currentValue = continuousDays;
                    targetValue = node.getRequiredDays();
                }
                if (node.getRequiredTradeCount() != null && node.getRequiredTradeCount() > 0) {
                    currentValue = totalTradeCount;
                    targetValue = node.getRequiredTradeCount();
                }
                if (node.getRequireVerification() != null && node.getRequireVerification() == 1) {
                    currentValue = userCredit.getIdentityVerified() == 1 ? 1 : 0;
                    targetValue = 1;
                }
                break;
        }

        boolean unlocked = currentValue >= targetValue;
        int status = unlocked ? CreditConstants.NODE_STATUS_UNLOCKED : CreditConstants.NODE_STATUS_LOCKED;
        String statusName = unlocked ? "已解锁" : "未解锁";
        double progress = targetValue > 0 ? Math.min(100.0, (currentValue * 100.0) / targetValue) : 100.0;

        return CreditNodeVO.builder()
                .id(node.getId())
                .nodeCode(node.getNodeCode())
                .nodeName(node.getNodeName())
                .nodeDesc(node.getNodeDesc())
                .icon(node.getIcon())
                .status(status)
                .statusName(statusName)
                .currentValue(currentValue)
                .targetValue(targetValue)
                .progress(Math.round(progress * 100) / 100.0)
                .sortOrder(node.getSortOrder())
                .build();
    }

    private String getCreditLevelName(Integer level) {
        if (level == null) {
            return CreditConstants.CREDIT_LEVEL_NAME_1;
        }
        Map<Integer, String> levelNames = Map.of(
                CreditConstants.CREDIT_LEVEL_1, CreditConstants.CREDIT_LEVEL_NAME_1,
                CreditConstants.CREDIT_LEVEL_2, CreditConstants.CREDIT_LEVEL_NAME_2,
                CreditConstants.CREDIT_LEVEL_3, CreditConstants.CREDIT_LEVEL_NAME_3,
                CreditConstants.CREDIT_LEVEL_4, CreditConstants.CREDIT_LEVEL_NAME_4,
                CreditConstants.CREDIT_LEVEL_5, CreditConstants.CREDIT_LEVEL_NAME_5
        );
        return levelNames.getOrDefault(level, CreditConstants.CREDIT_LEVEL_NAME_1);
    }

    private UserCredit getUserCredit(Long userId) {
        UserCredit userCredit = userCreditMapper.selectOne(
                new LambdaQueryWrapper<UserCredit>()
                        .eq(UserCredit::getUserId, userId)
                        .eq(UserCredit::getDeleted, 0)
        );

        if (userCredit == null) {
            userCredit = new UserCredit();
            userCredit.setUserId(userId);
            userCredit.setCreditScore(100);
            userCredit.setCreditLevel(CreditConstants.CREDIT_LEVEL_1);
            userCredit.setViolationCount(0);
            userCredit.setPendingViolationCount(0);
            userCredit.setContinuousFulfillDays(0);
            userCredit.setMaxContinuousFulfillDays(0);
            userCredit.setTotalFulfillCount(0);
            userCredit.setTotalTradeCount(0);
            userCredit.setIdentityVerified(0);
            userCredit.setPhoneVerified(0);
            userCredit.setEmailVerified(0);
            userCredit.setCreateTime(LocalDateTime.now());
            userCredit.setUpdateTime(LocalDateTime.now());
            userCreditMapper.insert(userCredit);
        }

        return userCredit;
    }

    private int countPendingViolations(Long userId) {
        Long count = creditViolationMapper.selectCount(
                new LambdaQueryWrapper<CreditViolation>()
                        .eq(CreditViolation::getUserId, userId)
                        .eq(CreditViolation::getStatus, CreditConstants.VIOLATION_STATUS_PENDING)
                        .eq(CreditViolation::getDeleted, 0)
        );
        return count != null ? count.intValue() : 0;
    }

    private LocalDateTime getFirstTradeTime(Long userId) {
        OrderInfo order = orderInfoMapper.selectOne(
                new LambdaQueryWrapper<OrderInfo>()
                        .and(wrapper -> wrapper
                                .eq(OrderInfo::getBuyerId, userId)
                                .or()
                                .eq(OrderInfo::getSellerId, userId)
                        )
                        .eq(OrderInfo::getDeleted, 0)
                        .ge(OrderInfo::getStatus, 4)
                        .orderByAsc(OrderInfo::getCreateTime)
                        .last("LIMIT 1")
        );
        return order != null ? order.getCreateTime() : null;
    }

    private int countTotalTrades(Long userId) {
        Long count = orderInfoMapper.selectCount(
                new LambdaQueryWrapper<OrderInfo>()
                        .and(wrapper -> wrapper
                                .eq(OrderInfo::getBuyerId, userId)
                                .or()
                                .eq(OrderInfo::getSellerId, userId)
                        )
                        .eq(OrderInfo::getDeleted, 0)
                        .ge(OrderInfo::getStatus, 4)
        );
        return count != null ? count.intValue() : 0;
    }

    private int countTotalFulfills(Long userId) {
        Long count = orderInfoMapper.selectCount(
                new LambdaQueryWrapper<OrderInfo>()
                        .and(wrapper -> wrapper
                                .eq(OrderInfo::getBuyerId, userId)
                                .or()
                                .eq(OrderInfo::getSellerId, userId)
                        )
                        .eq(OrderInfo::getDeleted, 0)
                        .eq(OrderInfo::getStatus, 4)
        );
        return count != null ? count.intValue() : 0;
    }

    private int calculateContinuousFulfillDays(Long userId) {
        List<OrderInfo> orders = orderInfoMapper.selectList(
                new LambdaQueryWrapper<OrderInfo>()
                        .and(wrapper -> wrapper
                                .eq(OrderInfo::getBuyerId, userId)
                                .or()
                                .eq(OrderInfo::getSellerId, userId)
                        )
                        .eq(OrderInfo::getDeleted, 0)
                        .eq(OrderInfo::getStatus, 4)
                        .orderByDesc(OrderInfo::getCompleteTime)
        );

        if (orders.isEmpty()) {
            return 0;
        }

        int continuousDays = 0;
        LocalDateTime currentDate = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime lastDate = null;

        for (OrderInfo order : orders) {
            LocalDateTime orderDate = order.getCompleteTime() != null
                    ? order.getCompleteTime().toLocalDate().atStartOfDay()
                    : null;

            if (orderDate == null) {
                continue;
            }

            if (lastDate == null) {
                if (orderDate.isEqual(currentDate) || orderDate.isEqual(currentDate.minusDays(1))) {
                    continuousDays = 1;
                    lastDate = orderDate;
                } else {
                    break;
                }
            } else {
                if (orderDate.isEqual(lastDate.minusDays(1))) {
                    continuousDays++;
                    lastDate = orderDate;
                } else if (!orderDate.isEqual(lastDate)) {
                    break;
                }
            }
        }

        return continuousDays;
    }

    private int getMaxContinuousDays(Long userId) {
        UserCredit userCredit = userCreditMapper.selectOne(
                new LambdaQueryWrapper<UserCredit>()
                        .eq(UserCredit::getUserId, userId)
                        .eq(UserCredit::getDeleted, 0)
        );
        return userCredit != null && userCredit.getMaxContinuousFulfillDays() != null
                ? userCredit.getMaxContinuousFulfillDays() : 0;
    }

    public void evictCreditCache(Long userId) {
        redisUtil.del(CreditConstants.CREDIT_CACHE_PREFIX + userId);
    }
}
