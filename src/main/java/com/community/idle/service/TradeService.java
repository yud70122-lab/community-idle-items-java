package com.community.idle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.idle.constants.CreditConstants;
import com.community.idle.constants.ItemConstants;
import com.community.idle.entity.Item;
import com.community.idle.entity.OrderInfo;
import com.community.idle.entity.User;
import com.community.idle.entity.UserCredit;
import com.community.idle.exception.BusinessException;
import com.community.idle.mapper.ItemMapper;
import com.community.idle.mapper.OrderInfoMapper;
import com.community.idle.mapper.UserCreditMapper;
import com.community.idle.mapper.UserMapper;
import com.community.idle.vo.TradeCheckVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class TradeService {

    private final ItemMapper itemMapper;
    private final UserMapper userMapper;
    private final UserCreditMapper userCreditMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final CreditService creditService;

    public TradeService(ItemMapper itemMapper, UserMapper userMapper,
                        UserCreditMapper userCreditMapper, OrderInfoMapper orderInfoMapper,
                        CreditService creditService) {
        this.itemMapper = itemMapper;
        this.userMapper = userMapper;
        this.userCreditMapper = userCreditMapper;
        this.orderInfoMapper = orderInfoMapper;
        this.creditService = creditService;
    }

    public TradeCheckVO checkExchangeEligibility(Long userId, Long itemId) {
        return checkTradeEligibility(userId, itemId, ItemConstants.TRADE_TYPE_EXCHANGE);
    }

    public TradeCheckVO checkBuyEligibility(Long userId, Long itemId) {
        return checkTradeEligibility(userId, itemId, ItemConstants.TRADE_TYPE_SELL);
    }

    private TradeCheckVO checkTradeEligibility(Long userId, Long itemId, Integer targetTradeType) {
        TradeCheckVO.TradeCheckVOBuilder builder = TradeCheckVO.builder();

        if (userId == null) {
            return builder
                    .canTrade(false)
                    .reason("请先登录")
                    .build();
        }

        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() != 1) {
            return builder
                    .canTrade(false)
                    .reason("用户状态异常，无法进行交易")
                    .build();
        }

        Item item = itemMapper.selectById(itemId);
        if (item == null) {
            return builder
                    .canTrade(false)
                    .reason("物品不存在")
                    .build();
        }

        builder.itemId(itemId)
                .itemTitle(item.getTitle())
                .price(item.getPrice())
                .shippingFee(BigDecimal.ZERO)
                .tradeType(item.getTradeType())
                .tradeTypeName(ItemConstants.getTradeTypeName(item.getTradeType()));

        if (!item.getStatus().equals(ItemConstants.STATUS_ON_SALE)) {
            return builder
                    .canTrade(false)
                    .reason("物品已下架或已售出")
                    .build();
        }

        if (!item.getTradeType().equals(targetTradeType)) {
            String reason = targetTradeType.equals(ItemConstants.TRADE_TYPE_EXCHANGE)
                    ? "该物品不支持以物换物"
                    : "该物品不支持直接购买";
            return builder
                    .canTrade(false)
                    .reason(reason)
                    .build();
        }

        if (item.getUserId().equals(userId)) {
            return builder
                    .canTrade(false)
                    .reason("不能交易自己发布的物品")
                    .build();
        }

        boolean hasPendingOrder = hasPendingOrder(userId, itemId);
        if (hasPendingOrder) {
            return builder
                    .canTrade(false)
                    .reason("您对此物品已有待处理订单，请先完成或取消")
                    .build();
        }

        UserCredit buyerCredit = getUserCredit(userId);
        int minScore = targetTradeType.equals(ItemConstants.TRADE_TYPE_EXCHANGE)
                ? ItemConstants.MIN_CREDIT_SCORE_FOR_EXCHANGE
                : ItemConstants.MIN_CREDIT_SCORE_FOR_BUY;

        builder.creditScore(buyerCredit.getCreditScore())
                .creditLevel(buyerCredit.getCreditLevel())
                .creditLevelName(getCreditLevelName(buyerCredit.getCreditLevel()))
                .minRequiredCreditScore(minScore)
                .identityVerified(buyerCredit.getIdentityVerified() == 1)
                .phoneVerified(user.getPhone() != null && buyerCredit.getPhoneVerified() == 1)
                .hasPendingViolation(buyerCredit.getPendingViolationCount() != null
                        && buyerCredit.getPendingViolationCount() > 0);

        if (buyerCredit.getCreditScore() < minScore) {
            return builder
                    .canTrade(false)
                    .reason(String.format("信用分不足，当前%d分，最低需要%d分",
                            buyerCredit.getCreditScore(), minScore))
                    .build();
        }

        if (buyerCredit.getPendingViolationCount() != null && buyerCredit.getPendingViolationCount() > 0) {
            return builder
                    .canTrade(false)
                    .reason("您有待处理的违规记录，请先处理后再进行交易")
                    .build();
        }

        User seller = userMapper.selectById(item.getUserId());
        if (seller != null) {
            builder.sellerId(seller.getId())
                    .sellerNickname(seller.getNickname())
                    .sellerAvatar(seller.getAvatar());

            UserCredit sellerCredit = getUserCredit(seller.getId());
            builder.sellerCreditScore(sellerCredit.getCreditScore());
        }

        creditService.evictCreditCache(userId);

        return builder
                .canTrade(true)
                .reason("校验通过，可以进行交易")
                .build();
    }

    private boolean hasPendingOrder(Long userId, Long itemId) {
        Long count = orderInfoMapper.selectCount(
                new LambdaQueryWrapper<OrderInfo>()
                        .eq(OrderInfo::getBuyerId, userId)
                        .eq(OrderInfo::getItemId, itemId)
                        .in(OrderInfo::getStatus,
                                ItemConstants.ORDER_STATUS_PENDING,
                                ItemConstants.ORDER_STATUS_PAID,
                                ItemConstants.ORDER_STATUS_SHIPPED,
                                ItemConstants.ORDER_STATUS_RECEIVED)
                        .eq(OrderInfo::getDeleted, 0)
        );
        return count != null && count > 0;
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

    public String generateOrderNo() {
        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "ORD" + timestamp + uuid;
    }
}
