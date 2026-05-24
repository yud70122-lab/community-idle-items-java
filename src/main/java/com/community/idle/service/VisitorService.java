package com.community.idle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.idle.constants.CreditConstants;
import com.community.idle.entity.Item;
import com.community.idle.entity.User;
import com.community.idle.entity.UserCredit;
import com.community.idle.entity.VisitorRecord;
import com.community.idle.exception.BusinessException;
import com.community.idle.mapper.ItemMapper;
import com.community.idle.mapper.UserCreditMapper;
import com.community.idle.mapper.UserMapper;
import com.community.idle.mapper.VisitorRecordMapper;
import com.community.idle.utils.RedisUtil;
import com.community.idle.vo.VisitorVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class VisitorService {

    private static final String VISITOR_LIST_KEY = "visitor:list:";
    private static final int VISITOR_LIST_LIMIT = 50;
    private static final int VISITOR_RETURN_LIMIT = 10;

    private final VisitorRecordMapper visitorRecordMapper;
    private final UserMapper userMapper;
    private final UserCreditMapper userCreditMapper;
    private final ItemMapper itemMapper;
    private final RedisUtil redisUtil;

    public VisitorService(VisitorRecordMapper visitorRecordMapper, UserMapper userMapper,
                          UserCreditMapper userCreditMapper, ItemMapper itemMapper,
                          RedisUtil redisUtil) {
        this.visitorRecordMapper = visitorRecordMapper;
        this.userMapper = userMapper;
        this.userCreditMapper = userCreditMapper;
        this.itemMapper = itemMapper;
        this.redisUtil = redisUtil;
    }

    @Async
    public void recordVisitor(Long userId, Long visitorId, Long itemId) {
        if (userId == null || visitorId == null || userId.equals(visitorId)) {
            return;
        }

        try {
            User visitor = userMapper.selectById(visitorId);
            if (visitor == null) {
                return;
            }

            UserCredit visitorCredit = userCreditMapper.selectOne(
                    new LambdaQueryWrapper<UserCredit>()
                            .eq(UserCredit::getUserId, visitorId)
                            .eq(UserCredit::getDeleted, 0)
            );

            String itemTitle = null;
            if (itemId != null) {
                Item item = itemMapper.selectById(itemId);
                if (item != null) {
                    itemTitle = item.getTitle();
                }
            }

            Integer creditLevel = visitorCredit != null ? visitorCredit.getCreditLevel() : CreditConstants.CREDIT_LEVEL_1;

            saveVisitorToRedis(userId, visitorId, itemId, visitor.getNickname(),
                    visitor.getAvatar(), creditLevel, itemTitle);

            VisitorRecord record = new VisitorRecord();
            record.setUserId(userId);
            record.setVisitorId(visitorId);
            record.setItemId(itemId);
            record.setVisitType(CreditConstants.VISIT_TYPE_ITEM);
            record.setVisitorNickname(visitor.getNickname());
            record.setVisitorAvatar(visitor.getAvatar());
            record.setVisitorCreditLevel(creditLevel);
            record.setVisitTime(LocalDateTime.now());
            visitorRecordMapper.insert(record);

            log.debug("访客记录保存成功, userId: {}, visitorId: {}, itemId: {}", userId, visitorId, itemId);

        } catch (Exception e) {
            log.error("保存访客记录异常, userId: {}, visitorId: {}", userId, visitorId, e);
        }
    }

    private void saveVisitorToRedis(Long userId, Long visitorId, Long itemId,
                                    String nickname, String avatar, Integer creditLevel,
                                    String itemTitle) {
        String key = VISITOR_LIST_KEY + userId;
        String value = visitorId + "|" + itemId + "|" + System.currentTimeMillis() + "|"
                + maskNickname(nickname) + "|" + avatar + "|" + creditLevel + "|" + itemTitle;

        Set<Object> existing = redisUtil.sGet(key);
        if (existing != null) {
            String prefixToRemove = visitorId + "|";
            List<Object> toRemove = existing.stream()
                    .filter(v -> v != null && v.toString().startsWith(prefixToRemove))
                    .collect(Collectors.toList());
            if (!toRemove.isEmpty()) {
                redisUtil.sRemove(key, toRemove.toArray());
            }
        }

        redisUtil.sSetAndTime(key, 7 * 24 * 3600, value);
    }

    public List<VisitorVO> getRecentVisitors(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        String key = VISITOR_LIST_KEY + userId;
        Set<Object> visitorSet = redisUtil.sGet(key);

        List<VisitorVO> result;
        if (visitorSet != null && !visitorSet.isEmpty()) {
            result = parseVisitorsFromRedis(visitorSet);
        } else {
            result = loadVisitorsFromDB(userId);
            cacheVisitorsToRedis(userId, result);
        }

        return result.stream()
                .sorted((v1, v2) -> v2.getVisitTime().compareTo(v1.getVisitTime()))
                .limit(VISITOR_RETURN_LIMIT)
                .collect(Collectors.toList());
    }

    private List<VisitorVO> parseVisitorsFromRedis(Set<Object> visitorSet) {
        List<VisitorVO> list = new ArrayList<>();
        for (Object obj : visitorSet) {
            if (obj == null) {
                continue;
            }
            try {
                String[] parts = obj.toString().split("\\|", -1);
                if (parts.length >= 6) {
                    VisitorVO vo = VisitorVO.builder()
                            .visitorId(Long.parseLong(parts[0]))
                            .itemId(!"null".equals(parts[1]) ? Long.parseLong(parts[1]) : null)
                            .visitTime(LocalDateTime.now())
                            .nickname(parts[3])
                            .avatar("null".equals(parts[4]) ? null : parts[4])
                            .creditLevel(Integer.parseInt(parts[5]))
                            .creditLevelName(getCreditLevelName(Integer.parseInt(parts[5])))
                            .itemTitle(parts.length > 6 && !"null".equals(parts[6]) ? parts[6] : null)
                            .build();
                    vo.setVisitTime(parseTimestamp(parts[2]));
                    list.add(vo);
                }
            } catch (Exception e) {
                log.warn("解析访客记录失败: {}", obj, e);
            }
        }
        return list;
    }

    private List<VisitorVO> loadVisitorsFromDB(Long userId) {
        List<VisitorRecord> records = visitorRecordMapper.selectList(
                new LambdaQueryWrapper<VisitorRecord>()
                        .eq(VisitorRecord::getUserId, userId)
                        .eq(VisitorRecord::getDeleted, 0)
                        .orderByDesc(VisitorRecord::getVisitTime)
                        .last("LIMIT " + VISITOR_LIST_LIMIT)
        );

        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, String> itemTitleMap = Collections.emptyMap();
        Set<Long> itemIds = records.stream()
                .map(VisitorRecord::getItemId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (!itemIds.isEmpty()) {
            List<Item> items = itemMapper.selectBatchIds(itemIds);
            itemTitleMap = items.stream()
                    .collect(Collectors.toMap(Item::getId, Item::getTitle, (a, b) -> a));
        }

        List<VisitorVO> result = new ArrayList<>();
        for (VisitorRecord record : records) {
            String nickname = record.getVisitorNickname() != null
                    ? maskNickname(record.getVisitorNickname())
                    : "匿名用户";

            Integer creditLevel = record.getVisitorCreditLevel() != null
                    ? record.getVisitorCreditLevel()
                    : CreditConstants.CREDIT_LEVEL_1;

            VisitorVO vo = VisitorVO.builder()
                    .visitorId(record.getVisitorId())
                    .nickname(nickname)
                    .avatar(record.getVisitorAvatar())
                    .creditLevel(creditLevel)
                    .creditLevelName(getCreditLevelName(creditLevel))
                    .itemId(record.getItemId())
                    .itemTitle(itemTitleMap.getOrDefault(record.getItemId(), null))
                    .visitTime(record.getVisitTime())
                    .build();
            result.add(vo);
        }

        return result;
    }

    private void cacheVisitorsToRedis(Long userId, List<VisitorVO> visitors) {
        if (visitors == null || visitors.isEmpty()) {
            return;
        }
        String key = VISITOR_LIST_KEY + userId;
        String[] values = visitors.stream()
                .map(v -> v.getVisitorId() + "|" + v.getItemId() + "|"
                        + v.getVisitTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() + "|"
                        + v.getNickname() + "|" + v.getAvatar() + "|"
                        + v.getCreditLevel() + "|" + v.getItemTitle())
                .toArray(String[]::new);
        redisUtil.sSetAndTime(key, 7 * 24 * 3600, values);
    }

    private String maskNickname(String nickname) {
        if (nickname == null || nickname.isEmpty()) {
            return "匿名用户";
        }
        if (nickname.length() <= 1) {
            return nickname + "**";
        }
        if (nickname.length() == 2) {
            return nickname.charAt(0) + "**";
        }
        return nickname.charAt(0) + "**" + nickname.charAt(nickname.length() - 1);
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

    private LocalDateTime parseTimestamp(String timestampStr) {
        try {
            long timestamp = Long.parseLong(timestampStr);
            return LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(timestamp),
                    java.time.ZoneId.systemDefault()
            );
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}
