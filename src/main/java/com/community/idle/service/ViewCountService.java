package com.community.idle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.community.idle.constants.ItemConstants;
import com.community.idle.entity.Item;
import com.community.idle.exception.BusinessException;
import com.community.idle.mapper.ItemMapper;
import com.community.idle.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class ViewCountService {

    private static final String VIEW_COUNT_KEY_PREFIX = ItemConstants.VIEW_COUNT_KEY_PREFIX;
    private static final String VIEW_COUNT_DIRTY_KEY = "item:view:dirty";
    private static final String VIEW_COUNT_INIT_KEY = "item:view:init";

    private final RedisUtil redisUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ItemMapper itemMapper;

    public ViewCountService(RedisUtil redisUtil, RedisTemplate<String, Object> redisTemplate, ItemMapper itemMapper) {
        this.redisUtil = redisUtil;
        this.redisTemplate = redisTemplate;
        this.itemMapper = itemMapper;
    }

    @PostConstruct
    public void init() {
        log.info("ViewCountService initialized, view count key prefix: {}", VIEW_COUNT_KEY_PREFIX);
        preloadViewCountsToRedis();
    }

    private void preloadViewCountsToRedis() {
        try {
            if (Boolean.TRUE.equals(redisUtil.hasKey(VIEW_COUNT_INIT_KEY))) {
                log.info("浏览量数据已预加载，跳过");
                return;
            }

            List<Item> items = itemMapper.selectList(new LambdaQueryWrapper<Item>()
                    .select(Item::getId, Item::getViewCount)
                    .eq(Item::getDeleted, 0));

            if (items == null || items.isEmpty()) {
                log.info("没有物品数据需要预加载");
                redisUtil.setString(VIEW_COUNT_INIT_KEY, "1");
                return;
            }

            int successCount = 0;
            for (Item item : items) {
                try {
                    String key = VIEW_COUNT_KEY_PREFIX + item.getId();
                    if (!redisUtil.hasKey(key) && item.getViewCount() != null) {
                        redisUtil.setString(key, String.valueOf(item.getViewCount()));
                        successCount++;
                    }
                } catch (Exception e) {
                    log.warn("预加载物品浏览量失败, itemId: {}", item.getId(), e);
                }
            }

            redisUtil.setString(VIEW_COUNT_INIT_KEY, "1");
            log.info("浏览量数据预加载完成，共加载 {} 条记录", successCount);
        } catch (Exception e) {
            log.error("预加载浏览量数据失败", e);
        }
    }

    @Async
    public void incrementViewCount(Long itemId) {
        if (itemId == null) {
            return;
        }

        try {
            String key = VIEW_COUNT_KEY_PREFIX + itemId;

            long newCount = redisTemplate.execute((RedisCallback<Long>) connection -> {
                byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
                return connection.incr(keyBytes);
            });

            if (newCount == 1) {
                Item item = itemMapper.selectById(itemId);
                if (item == null) {
                    redisUtil.del(key);
                    throw new BusinessException("物品不存在");
                }
                if (item.getViewCount() != null && item.getViewCount() > 0) {
                    int dbCount = item.getViewCount() + 1;
                    redisUtil.setString(key, String.valueOf(dbCount));
                    newCount = dbCount;
                }
            }

            redisUtil.sSet(VIEW_COUNT_DIRTY_KEY, itemId.toString());

            log.debug("浏览量增加成功, itemId: {}, 新值: {}", itemId, newCount);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("浏览量增加失败, itemId: {}", itemId, e);
            try {
                itemMapper.update(null, new LambdaUpdateWrapper<Item>()
                        .eq(Item::getId, itemId)
                        .setSql("view_count = view_count + 1"));
                log.info("降级处理：直接更新MySQL浏览量, itemId: {}", itemId);
            } catch (Exception ex) {
                log.error("直接更新MySQL浏览量也失败, itemId: {}", itemId, ex);
            }
        }
    }

    public int getCurrentViewCount(Long itemId) {
        String key = VIEW_COUNT_KEY_PREFIX + itemId;
        String value = redisUtil.getString(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                log.warn("Redis浏览量格式异常, itemId: {}, value: {}", itemId, value);
            }
        }
        Item item = itemMapper.selectById(itemId);
        return item != null && item.getViewCount() != null ? item.getViewCount() : 0;
    }

    @Scheduled(cron = "0 */5 * * * ?")
    public void syncViewCountsToDB() {
        log.info("开始执行定时任务：同步浏览量到MySQL");

        Set<Object> dirtyItemIds = redisUtil.sGet(VIEW_COUNT_DIRTY_KEY);
        if (dirtyItemIds == null || dirtyItemIds.isEmpty()) {
            log.info("没有需要同步的浏览量数据");
            return;
        }

        log.info("需要同步的物品数量: {}", dirtyItemIds.size());

        int successCount = 0;
        int failCount = 0;

        for (Object obj : dirtyItemIds) {
            String itemIdStr = obj.toString();
            try {
                Long itemId = Long.parseLong(itemIdStr);
                String key = VIEW_COUNT_KEY_PREFIX + itemId;
                String valueStr = redisUtil.getString(key);

                if (valueStr == null) {
                    redisUtil.sRemove(VIEW_COUNT_DIRTY_KEY, itemIdStr);
                    continue;
                }

                int viewCount = Integer.parseInt(valueStr);

                itemMapper.update(null, new LambdaUpdateWrapper<Item>()
                        .eq(Item::getId, itemId)
                        .set(Item::getViewCount, viewCount));

                redisUtil.sRemove(VIEW_COUNT_DIRTY_KEY, itemIdStr);
                successCount++;

                log.debug("同步浏览量成功, itemId: {}, count: {}", itemId, viewCount);

            } catch (Exception e) {
                failCount++;
                log.error("同步浏览量失败, itemId: {}", itemIdStr, e);
            }
        }

        log.info("定时任务执行完成：同步浏览量到MySQL，成功: {}, 失败: {}", successCount, failCount);
    }

    public Map<String, Integer> getAllViewCountsFromRedis() {
        Map<String, Integer> result = new HashMap<>();
        Set<Object> dirtyItemIds = redisUtil.sGet(VIEW_COUNT_DIRTY_KEY);
        if (dirtyItemIds == null) {
            return result;
        }

        for (Object obj : dirtyItemIds) {
            String itemIdStr = obj.toString();
            String key = VIEW_COUNT_KEY_PREFIX + itemIdStr;
            String value = redisUtil.getString(key);
            if (value != null) {
                try {
                    result.put(itemIdStr, Integer.parseInt(value));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return result;
    }
}
