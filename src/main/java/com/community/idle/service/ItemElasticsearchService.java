package com.community.idle.service;

import com.community.idle.constants.ItemConstants;
import com.community.idle.entity.Item;
import com.community.idle.entity.ItemDoc;
import com.community.idle.entity.User;
import com.community.idle.exception.BusinessException;
import com.community.idle.mapper.ItemMapper;
import com.community.idle.mapper.UserMapper;
import com.community.idle.repository.ItemRepository;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
public class ItemElasticsearchService {

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;
    private final UserMapper userMapper;

    private static final Map<Long, String> CATEGORY_NAMES = new HashMap<>();

    static {
        CATEGORY_NAMES.put(1L, "数码电子");
        CATEGORY_NAMES.put(2L, "家居用品");
        CATEGORY_NAMES.put(3L, "服饰鞋包");
        CATEGORY_NAMES.put(4L, "图书文具");
        CATEGORY_NAMES.put(5L, "母婴用品");
        CATEGORY_NAMES.put(6L, "运动户外");
        CATEGORY_NAMES.put(7L, "美妆个护");
        CATEGORY_NAMES.put(8L, "其他");
    }

    public ItemElasticsearchService(ItemRepository itemRepository, ItemMapper itemMapper, UserMapper userMapper) {
        this.itemRepository = itemRepository;
        this.itemMapper = itemMapper;
        this.userMapper = userMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public void syncItemById(Long itemId) {
        Item item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException("物品不存在");
        }
        syncItem(item);
    }

    @Transactional(rollbackFor = Exception.class)
    public void syncItem(Item item) {
        if (item == null) {
            return;
        }

        ItemDoc doc = convertToDoc(item);

        if (item.getDeleted() != null && item.getDeleted() == 1) {
            itemRepository.deleteById(item.getId());
        } else {
            itemRepository.save(doc);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteItem(Long itemId) {
        itemRepository.deleteById(itemId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchSyncItems(java.util.List<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return;
        }
        java.util.List<Item> items = itemMapper.selectBatchIds(itemIds);
        for (Item item : items) {
            syncItem(item);
        }
    }

    private ItemDoc convertToDoc(Item item) {
        ItemDoc doc = new ItemDoc();
        doc.setId(item.getId());
        doc.setUserId(item.getUserId());
        doc.setTitle(item.getTitle());
        doc.setDescription(item.getDescription());
        doc.setCategoryId(item.getCategoryId());
        doc.setCategoryName(CATEGORY_NAMES.getOrDefault(item.getCategoryId(), "其他"));
        doc.setPrice(item.getPrice());
        doc.setOriginalPrice(item.getOriginalPrice());
        doc.setCoverImage(item.getCoverImage());
        doc.setImages(item.getImages());
        doc.setCondition(item.getCondition());
        doc.setConditionName(ItemConstants.getConditionName(item.getCondition()));
        doc.setTradeType(item.getTradeType());
        doc.setTradeTypeName(ItemConstants.getTradeTypeName(item.getTradeType()));
        doc.setStatus(item.getStatus());
        doc.setStatusName(ItemConstants.getStatusName(item.getStatus()));
        doc.setViewCount(item.getViewCount());
        doc.setLikeCount(item.getLikeCount());
        doc.setFavoriteCount(item.getFavoriteCount());
        doc.setLocationName(item.getLocation());
        doc.setLatitude(item.getLatitude());
        doc.setLongitude(item.getLongitude());
        doc.setCreateTime(item.getCreateTime());
        doc.setUpdateTime(item.getUpdateTime());
        doc.setDeleted(item.getDeleted());

        if (item.getLatitude() != null && item.getLongitude() != null) {
            doc.setLocation(new GeoPoint(item.getLatitude().doubleValue(), item.getLongitude().doubleValue()));
        }

        User user = userMapper.selectById(item.getUserId());
        if (user != null) {
            doc.setNickname(user.getNickname());
            doc.setAvatar(user.getAvatar());
        }

        return doc;
    }
}
