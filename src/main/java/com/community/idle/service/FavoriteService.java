package com.community.idle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.community.idle.constants.ItemConstants;
import com.community.idle.entity.Favorite;
import com.community.idle.entity.Item;
import com.community.idle.exception.BusinessException;
import com.community.idle.mapper.FavoriteMapper;
import com.community.idle.mapper.ItemMapper;
import com.community.idle.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final ItemMapper itemMapper;
    private final RedisUtil redisUtil;

    public FavoriteService(FavoriteMapper favoriteMapper, ItemMapper itemMapper, RedisUtil redisUtil) {
        this.favoriteMapper = favoriteMapper;
        this.itemMapper = itemMapper;
        this.redisUtil = redisUtil;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> addFavorite(Long userId, Long itemId) {
        Item item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException("物品不存在");
        }

        String favoriteKey = ItemConstants.FAVORITE_KEY_PREFIX + itemId;

        Boolean isMember = redisUtil.sHasKey(favoriteKey, userId.toString());
        Map<String, Object> result = new HashMap<>();

        if (Boolean.TRUE.equals(isMember)) {
            redisUtil.sRemove(favoriteKey, userId.toString());
            favoriteMapper.delete(new LambdaQueryWrapper<Favorite>()
                    .eq(Favorite::getUserId, userId)
                    .eq(Favorite::getItemId, itemId));
            itemMapper.update(null, new LambdaUpdateWrapper<Item>()
                    .eq(Item::getId, itemId)
                    .setSql("favorite_count = favorite_count - 1"));
            result.put("favorited", false);
            result.put("message", "取消收藏成功");
        } else {
            redisUtil.sSet(favoriteKey, userId.toString());
            Favorite favorite = new Favorite();
            favorite.setUserId(userId);
            favorite.setItemId(itemId);
            favorite.setCreateTime(LocalDateTime.now());
            favoriteMapper.insert(favorite);
            itemMapper.update(null, new LambdaUpdateWrapper<Item>()
                    .eq(Item::getId, itemId)
                    .setSql("favorite_count = favorite_count + 1"));
            result.put("favorited", true);
            result.put("message", "收藏成功");
        }

        Item updatedItem = itemMapper.selectById(itemId);
        result.put("favoriteCount", updatedItem != null && updatedItem.getFavoriteCount() != null
                ? updatedItem.getFavoriteCount() : 0);

        return result;
    }

    public boolean isFavorited(Long userId, Long itemId) {
        if (userId == null || itemId == null) {
            return false;
        }
        String favoriteKey = ItemConstants.FAVORITE_KEY_PREFIX + itemId;
        return Boolean.TRUE.equals(redisUtil.sHasKey(favoriteKey, userId.toString()));
    }
}
