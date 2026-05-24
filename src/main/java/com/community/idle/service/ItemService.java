package com.community.idle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.idle.constants.ItemConstants;
import com.community.idle.dto.ItemEditDTO;
import com.community.idle.dto.ItemPublishDTO;
import com.community.idle.dto.ItemQueryDTO;
import com.community.idle.entity.Item;
import com.community.idle.entity.User;
import com.community.idle.exception.BusinessException;
import com.community.idle.mapper.ItemMapper;
import com.community.idle.mapper.UserMapper;
import com.community.idle.utils.RedisUtil;
import com.community.idle.utils.WxSubMsgUtil;
import com.community.idle.vo.GenDescVO;
import com.community.idle.vo.ItemDetailVO;
import com.community.idle.vo.ItemListVO;
import com.community.idle.vo.PriceRefVO;
import com.community.idle.vo.TagRecommendVO;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ItemService {

    private final ItemMapper itemMapper;
    private final UserMapper userMapper;
    private final RedisUtil redisUtil;
    private final WxSubMsgUtil wxSubMsgUtil;
    private final AsyncAuditService asyncAuditService;
    private final FavoriteService favoriteService;

    private static final Map<Long, String> CATEGORY_NAMES = new HashMap<>();
    private static final Map<String, List<String>> KEYWORD_TAG_MAP = new HashMap<>();
    private static final Map<Long, String> DESC_TEMPLATES = new HashMap<>();

    static {
        CATEGORY_NAMES.put(1L, "数码电子");
        CATEGORY_NAMES.put(2L, "家居用品");
        CATEGORY_NAMES.put(3L, "服饰鞋包");
        CATEGORY_NAMES.put(4L, "图书文具");
        CATEGORY_NAMES.put(5L, "母婴用品");
        CATEGORY_NAMES.put(6L, "运动户外");
        CATEGORY_NAMES.put(7L, "美妆个护");
        CATEGORY_NAMES.put(8L, "其他");

        KEYWORD_TAG_MAP.put("手机", Arrays.asList("手机", "数码", "通讯"));
        KEYWORD_TAG_MAP.put("电脑", Arrays.asList("电脑", "数码", "办公"));
        KEYWORD_TAG_MAP.put("笔记本", Arrays.asList("笔记本", "电脑", "数码"));
        KEYWORD_TAG_MAP.put("平板", Arrays.asList("平板", "数码", "电子"));
        KEYWORD_TAG_MAP.put("相机", Arrays.asList("相机", "数码", "摄影"));
        KEYWORD_TAG_MAP.put("耳机", Arrays.asList("耳机", "数码", "音频"));
        KEYWORD_TAG_MAP.put("键盘", Arrays.asList("键盘", "数码", "外设"));
        KEYWORD_TAG_MAP.put("鼠标", Arrays.asList("鼠标", "数码", "外设"));
        KEYWORD_TAG_MAP.put("显示器", Arrays.asList("显示器", "数码", "办公"));
        KEYWORD_TAG_MAP.put("椅子", Arrays.asList("椅子", "家居", "办公"));
        KEYWORD_TAG_MAP.put("桌子", Arrays.asList("桌子", "家居", "家具"));
        KEYWORD_TAG_MAP.put("沙发", Arrays.asList("沙发", "家居", "家具"));
        KEYWORD_TAG_MAP.put("床", Arrays.asList("床", "家居", "家具"));
        KEYWORD_TAG_MAP.put("衣服", Arrays.asList("服饰", "穿搭", "服装"));
        KEYWORD_TAG_MAP.put("鞋子", Arrays.asList("鞋子", "服饰", "穿搭"));
        KEYWORD_TAG_MAP.put("包包", Arrays.asList("包包", "服饰", "配饰"));
        KEYWORD_TAG_MAP.put("书", Arrays.asList("图书", "阅读", "学习"));
        KEYWORD_TAG_MAP.put("课本", Arrays.asList("课本", "教材", "学习"));
        KEYWORD_TAG_MAP.put("婴儿", Arrays.asList("母婴", "婴儿", "育儿"));
        KEYWORD_TAG_MAP.put("儿童", Arrays.asList("母婴", "儿童", "玩具"));
        KEYWORD_TAG_MAP.put("篮球", Arrays.asList("篮球", "运动", "户外"));
        KEYWORD_TAG_MAP.put("足球", Arrays.asList("足球", "运动", "户外"));
        KEYWORD_TAG_MAP.put("瑜伽", Arrays.asList("瑜伽", "运动", "健身"));
        KEYWORD_TAG_MAP.put("面膜", Arrays.asList("面膜", "美妆", "护肤"));
        KEYWORD_TAG_MAP.put("口红", Arrays.asList("口红", "美妆", "彩妆"));
        KEYWORD_TAG_MAP.put("九成新", Arrays.asList("九成新", "品相好"));
        KEYWORD_TAG_MAP.put("全新", Arrays.asList("全新", "未拆封"));
        KEYWORD_TAG_MAP.put("自用", Arrays.asList("自用", "个人闲置"));
        KEYWORD_TAG_MAP.put("正品", Arrays.asList("正品", "保真"));
        KEYWORD_TAG_MAP.put("包邮", Arrays.asList("包邮", "优惠"));

        DESC_TEMPLATES.put(1L, "自用{condition}的{category}，{sellingPoints}。功能完好，无质量问题，平时爱惜使用，保养得当。因{reason}转手，希望找到有缘人继续使用。支持当面验货，非人为损坏可提供售后咨询。");
        DESC_TEMPLATES.put(2L, "{condition}{category}转让，{sellingPoints}。质量可靠，使用时间不长，性能稳定。适合居家使用，性价比很高。因搬家/闲置处理，价格可小刀，自提优先。");
        DESC_TEMPLATES.put(3L, "{condition}的{category}，{sellingPoints}。款式好看，搭配性强，穿上很显气质。因身材变化/风格转变转出，已清洗干净，放心购买。");
        DESC_TEMPLATES.put(4L, "{condition}{category}，{sellingPoints}。内容丰富，知识含金量高，适合学习提升。笔记工整，无涂画破损，看完转出给需要的朋友。");
        DESC_TEMPLATES.put(5L, "{condition}的{category}，{sellingPoints}。宝宝长大了用不上，转给有需要的家庭。材质安全，清洁消毒到位，宝宝用得放心。");
        DESC_TEMPLATES.put(6L, "{condition}{category}装备，{sellingPoints}。质量过硬，户外运动必备。使用次数不多，性能依然出色，热爱运动的朋友不要错过。");
        DESC_TEMPLATES.put(7L, "{condition}的{category}，{sellingPoints}。效果很好，个人肤质不适合/囤货太多转出。保质期充足，正品保障，美妆爱好者可以入手。");
        DESC_TEMPLATES.put(8L, "{condition}{category}，{sellingPoints}。实用好物，因闲置转手。功能正常，品相完好，需要的朋友可以联系我详谈。");
    }

    public ItemService(ItemMapper itemMapper, UserMapper userMapper, RedisUtil redisUtil, WxSubMsgUtil wxSubMsgUtil,
                       AsyncAuditService asyncAuditService, FavoriteService favoriteService) {
        this.itemMapper = itemMapper;
        this.userMapper = userMapper;
        this.redisUtil = redisUtil;
        this.wxSubMsgUtil = wxSubMsgUtil;
        this.asyncAuditService = asyncAuditService;
        this.favoriteService = favoriteService;
    }

    @Transactional(rollbackFor = Exception.class)
    public Item publishItem(Long userId, ItemPublishDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() != 1) {
            throw new BusinessException("用户状态异常，无法发布物品");
        }

        if (dto.getTradeType() == ItemConstants.TRADE_TYPE_EXCHANGE
                && (dto.getExpectExchange() == null || dto.getExpectExchange().trim().isEmpty())) {
            throw new BusinessException("以物换物请填写期望交换物品描述");
        }

        Item item = new Item();
        item.setUserId(userId);
        item.setTitle(dto.getTitle());
        item.setDescription(dto.getDescription());
        item.setCategoryId(dto.getCategoryId());
        item.setPrice(dto.getPrice() != null ? dto.getPrice() : BigDecimal.ZERO);
        item.setOriginalPrice(dto.getOriginalPrice());
        item.setCoverImage(dto.getCoverImage());
        item.setImages(dto.getImages());
        item.setCondition(dto.getCondition());
        item.setTradeType(dto.getTradeType());
        item.setStatus(ItemConstants.STATUS_REVIEW);
        item.setViewCount(0);
        item.setLikeCount(0);

        itemMapper.insert(item);

        asyncAuditService.auditItemContent(item.getId());

        return item;
    }

    @Transactional(rollbackFor = Exception.class)
    public void auditItem(Long itemId, boolean approved, String remark, Long auditorId) {
        Item item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException("物品不存在");
        }
        if (item.getStatus() != ItemConstants.STATUS_REVIEW) {
            throw new BusinessException("物品状态不是审核中");
        }

        int newStatus = approved ? ItemConstants.STATUS_ON_SALE : ItemConstants.STATUS_OFFLINE;
        itemMapper.update(null, new LambdaUpdateWrapper<Item>()
                .eq(Item::getId, itemId)
                .set(Item::getStatus, newStatus));

        User user = userMapper.selectById(item.getUserId());
        if (user != null && user.getOpenid() != null) {
            String result = approved ? "审核通过" : "审核未通过";
            String reviewTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            String reason = remark != null ? remark : (approved ? "物品信息符合规范" : "请完善物品描述和图片");

            wxSubMsgUtil.sendReviewResult(
                    user.getOpenid(),
                    item.getTitle(),
                    result,
                    reviewTime,
                    reason
            );
        }
    }

    public Page<ItemListVO> getItemList(Integer status, Long categoryId, String keyword,
                                        Long pageNum, Long pageSize, Long currentUserId) {
        ItemQueryDTO query = new ItemQueryDTO();
        query.setStatus(status);
        query.setCategoryId(categoryId);
        query.setKeyword(keyword);
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        query.setCurrentUserId(currentUserId);
        return getItemList(query);
    }

    public Page<ItemListVO> getItemList(ItemQueryDTO query) {
        if (query.getPageNum() == null) query.setPageNum(1L);
        if (query.getPageSize() == null) query.setPageSize(20L);
        if (query.getSortBy() == null) query.setSortBy("match");

        Page<Item> itemPage = itemMapper.selectItemList(new Page<>(query.getPageNum(), query.getPageSize()), query);

        Set<Long> userIds = itemPage.getRecords().stream()
                .map(Item::getUserId)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));
        }

        List<ItemListVO> voList = new ArrayList<>();
        for (Item item : itemPage.getRecords()) {
            User user = userMap.get(item.getUserId());
            ItemListVO vo = convertToVO(item, user, query.getCurrentUserId());
            voList.add(vo);
        }

        Page<ItemListVO> voPage = new Page<>(itemPage.getCurrent(), itemPage.getSize(), itemPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    public Page<ItemListVO> getMyItems(Long userId, Integer status, Long pageNum, Long pageSize) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() != 1) {
            throw new BusinessException("用户状态异常");
        }

        List<Item> items = itemMapper.selectMyItems(userId, status, (pageNum - 1) * pageSize, pageSize);
        long total = itemMapper.countMyItems(userId, status);

        List<ItemListVO> voList = new ArrayList<>();
        for (Item item : items) {
            ItemListVO vo = convertToVO(item, user, userId);
            voList.add(vo);
        }

        Page<ItemListVO> voPage = new Page<>(pageNum, pageSize, total);
        voPage.setRecords(voList);
        return voPage;
    }

    public boolean toggleLike(Long userId, Long itemId) {
        Item item = itemMapper.selectById(itemId);
        if (item == null || item.getStatus() != ItemConstants.STATUS_ON_SALE) {
            throw new BusinessException("物品不存在或已下架");
        }

        String likeKey = ItemConstants.LIKE_KEY_PREFIX + itemId;
        Boolean isMember = redisUtil.sHasKey(likeKey, userId.toString());

        if (Boolean.TRUE.equals(isMember)) {
            redisUtil.sRemove(likeKey, userId.toString());
            itemMapper.update(null, new LambdaUpdateWrapper<Item>()
                    .eq(Item::getId, itemId)
                    .setSql("like_count = like_count - 1"));
            return false;
        } else {
            redisUtil.sSet(likeKey, userId.toString());
            itemMapper.update(null, new LambdaUpdateWrapper<Item>()
                    .eq(Item::getId, itemId)
                    .setSql("like_count = like_count + 1"));
            return true;
        }
    }

    @Async
    public void notifyTradeSuccess(Long itemId, Long buyerId) {
        Item item = itemMapper.selectById(itemId);
        if (item == null) {
            return;
        }

        User seller = userMapper.selectById(item.getUserId());
        User buyer = userMapper.selectById(buyerId);

        String tradeTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        String amount = item.getPrice() != null ? item.getPrice().toPlainString() : "面议";

        if (seller != null && seller.getOpenid() != null) {
            String buyerName = buyer != null ? buyer.getNickname() : "匿名用户";
            wxSubMsgUtil.sendTradeSuccess(
                    seller.getOpenid(),
                    item.getTitle(),
                    buyerName,
                    amount,
                    tradeTime
            );
        }
    }

    public TagRecommendVO recommendTags(String title, String description) {
        Set<String> tagSet = new LinkedHashSet<>();

        String content = (title != null ? title : "") + " " + (description != null ? description : "");
        content = content.toLowerCase();

        for (Map.Entry<String, List<String>> entry : KEYWORD_TAG_MAP.entrySet()) {
            if (content.contains(entry.getKey())) {
                tagSet.addAll(entry.getValue());
            }
        }

        if (content.contains("新") || content.contains("全新")) {
            tagSet.add("品相好");
        }
        if (content.contains("包邮") || content.contains("免运费")) {
            tagSet.add("包邮");
        }
        if (content.contains("小刀") || content.contains("议价") || content.contains("可谈")) {
            tagSet.add("可小刀");
        }
        if (content.contains("自提") || content.contains("自取")) {
            tagSet.add("自提优先");
        }

        List<String> commonTags = Arrays.asList("个人闲置", "正品保障", "性价比高", "值得入手", "好物推荐");
        Random random = new Random();
        while (tagSet.size() < 6 && !commonTags.isEmpty()) {
            int index = random.nextInt(commonTags.size());
            tagSet.add(commonTags.get(index));
            commonTags.remove(index);
        }

        List<String> tags = new ArrayList<>(tagSet);
        if (tags.size() > 10) {
            tags = tags.subList(0, 10);
        }

        return TagRecommendVO.builder().tags(tags).build();
    }

    public PriceRefVO getPriceReference(String itemName) {
        if (!StringUtils.hasText(itemName)) {
            throw new BusinessException("物品名称不能为空");
        }

        Map<String, BigDecimal> priceStats = itemMapper.selectPriceStatsByItemName(itemName);
        int tradeCount = itemMapper.countSoldItemsByName(itemName);

        BigDecimal minPrice = priceStats != null ? priceStats.get("minPrice") : null;
        BigDecimal maxPrice = priceStats != null ? priceStats.get("maxPrice") : null;
        BigDecimal avgPrice = priceStats != null ? priceStats.get("avgPrice") : null;

        String referenceRange;
        if (minPrice == null || maxPrice == null) {
            BigDecimal estimated = estimatePrice(itemName);
            minPrice = estimated.multiply(new BigDecimal("0.7")).setScale(2, RoundingMode.HALF_UP);
            maxPrice = estimated.multiply(new BigDecimal("1.3")).setScale(2, RoundingMode.HALF_UP);
            avgPrice = estimated;
            referenceRange = minPrice + "-" + maxPrice + "元（参考）";
            tradeCount = 0;
        } else {
            avgPrice = avgPrice.setScale(2, RoundingMode.HALF_UP);
            referenceRange = minPrice + "-" + maxPrice + "元";
        }

        return PriceRefVO.builder()
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .avgPrice(avgPrice)
                .referenceRange(referenceRange)
                .tradeCount(tradeCount)
                .build();
    }

    private BigDecimal estimatePrice(String itemName) {
        String lowerName = itemName.toLowerCase();
        if (lowerName.contains("手机") || lowerName.contains("iphone")) {
            return new BigDecimal("2000");
        } else if (lowerName.contains("电脑") || lowerName.contains("笔记本")) {
            return new BigDecimal("3000");
        } else if (lowerName.contains("平板") || lowerName.contains("ipad")) {
            return new BigDecimal("1500");
        } else if (lowerName.contains("耳机")) {
            return new BigDecimal("300");
        } else if (lowerName.contains("相机")) {
            return new BigDecimal("2500");
        } else if (lowerName.contains("桌子") || lowerName.contains("椅子") || lowerName.contains("家具")) {
            return new BigDecimal("200");
        } else if (lowerName.contains("衣服") || lowerName.contains("鞋子") || lowerName.contains("包包")) {
            return new BigDecimal("150");
        } else if (lowerName.contains("书") || lowerName.contains("课本")) {
            return new BigDecimal("30");
        } else {
            return new BigDecimal("100");
        }
    }

    public GenDescVO generateDescription(Long categoryId, Integer condition, List<String> sellingPoints) {
        if (categoryId == null) {
            throw new BusinessException("品类不能为空");
        }
        if (condition == null) {
            throw new BusinessException("成色不能为空");
        }

        String categoryName = CATEGORY_NAMES.getOrDefault(categoryId, "物品");
        String conditionName = ItemConstants.getConditionName(condition);
        String sellingPointsStr = sellingPoints != null && !sellingPoints.isEmpty()
                ? String.join("，", sellingPoints)
                : "品相完好，功能正常";

        String template = DESC_TEMPLATES.getOrDefault(categoryId, DESC_TEMPLATES.get(8L));

        String description = template
                .replace("{category}", categoryName)
                .replace("{condition}", conditionName)
                .replace("{sellingPoints}", sellingPointsStr)
                .replace("{reason}", getRandomReason());

        return GenDescVO.builder().description(description).build();
    }

    private String getRandomReason() {
        List<String> reasons = Arrays.asList(
                "升级换代",
                "闲置不用",
                "搬家清理",
                "买多了",
                "用不上了",
                "冲动消费",
                "兴趣转移",
                "空间有限"
        );
        Random random = new Random();
        return reasons.get(random.nextInt(reasons.size()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void offlineItem(Long userId, Long itemId) {
        Item item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException("物品不存在");
        }
        if (!item.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此物品");
        }
        if (item.getStatus().equals(ItemConstants.STATUS_SOLD)) {
            throw new BusinessException("已售出物品不能下架");
        }
        if (item.getStatus().equals(ItemConstants.STATUS_OFFLINE)) {
            throw new BusinessException("物品已下架");
        }

        itemMapper.update(null, new LambdaUpdateWrapper<Item>()
                .eq(Item::getId, itemId)
                .set(Item::getStatus, ItemConstants.STATUS_OFFLINE)
                .set(Item::getUpdateTime, LocalDateTime.now()));
    }

    public ItemDetailVO getItemDetailForEdit(Long userId, Long itemId) {
        Item item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException("物品不存在");
        }
        if (!item.getUserId().equals(userId)) {
            throw new BusinessException("无权查看此物品");
        }

        return ItemDetailVO.builder()
                .id(item.getId())
                .userId(item.getUserId())
                .title(item.getTitle())
                .description(item.getDescription())
                .categoryId(item.getCategoryId())
                .price(item.getPrice())
                .originalPrice(item.getOriginalPrice())
                .coverImage(item.getCoverImage())
                .images(item.getImages())
                .condition(item.getCondition())
                .tradeType(item.getTradeType())
                .status(item.getStatus())
                .createTime(item.getCreateTime())
                .updateTime(item.getUpdateTime())
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public Item updateItem(Long userId, Long itemId, ItemEditDTO dto) {
        Item item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException("物品不存在");
        }
        if (!item.getUserId().equals(userId)) {
            throw new BusinessException("无权编辑此物品");
        }
        if (item.getStatus().equals(ItemConstants.STATUS_SOLD)) {
            throw new BusinessException("已售出物品不能编辑");
        }
        if (item.getStatus().equals(ItemConstants.STATUS_LOCKED)) {
            throw new BusinessException("已锁定物品不能编辑");
        }

        if (dto.getTradeType() == ItemConstants.TRADE_TYPE_EXCHANGE
                && (dto.getExpectExchange() == null || dto.getExpectExchange().trim().isEmpty())) {
            throw new BusinessException("以物换物请填写期望交换物品描述");
        }

        item.setTitle(dto.getTitle());
        item.setDescription(dto.getDescription());
        item.setCategoryId(dto.getCategoryId());
        item.setPrice(dto.getPrice() != null ? dto.getPrice() : BigDecimal.ZERO);
        item.setOriginalPrice(dto.getOriginalPrice());
        item.setCoverImage(dto.getCoverImage());
        item.setImages(dto.getImages());
        item.setCondition(dto.getCondition());
        item.setTradeType(dto.getTradeType());
        item.setUpdateTime(LocalDateTime.now());

        if (item.getStatus().equals(ItemConstants.STATUS_OFFLINE)) {
            item.setStatus(ItemConstants.STATUS_REVIEW);
        } else {
            item.setStatus(ItemConstants.STATUS_REVIEW);
        }

        itemMapper.updateById(item);

        asyncAuditService.auditItemContent(item.getId());

        return item;
    }

    public List<ItemListVO> getSimilarItems(Long itemId, Long currentUserId, Integer limit) {
        Item item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException("物品不存在");
        }

        if (limit == null || limit <= 0) {
            limit = 10;
        }

        Long categoryId = item.getCategoryId();
        java.math.BigDecimal price = item.getPrice();
        java.math.BigDecimal minPrice = null;
        java.math.BigDecimal maxPrice = null;

        if (price != null && price.compareTo(java.math.BigDecimal.ZERO) > 0) {
            minPrice = price.multiply(new java.math.BigDecimal("0.7")).setScale(2, java.math.RoundingMode.HALF_UP);
            maxPrice = price.multiply(new java.math.BigDecimal("1.3")).setScale(2, java.math.RoundingMode.HALF_UP);
        }

        String keyword = extractKeyword(item.getTitle());

        List<Item> similarItems = itemMapper.selectSimilarItems(itemId, categoryId, minPrice, maxPrice, keyword, limit);

        if (similarItems == null || similarItems.isEmpty()) {
            similarItems = itemMapper.selectSimilarItems(itemId, categoryId, null, null, null, limit);
        }

        if (similarItems == null || similarItems.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Long> userIds = similarItems.stream()
                .map(Item::getUserId)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));
        }

        List<ItemListVO> voList = new ArrayList<>();
        for (Item similarItem : similarItems) {
            User user = userMap.get(similarItem.getUserId());
            ItemListVO vo = convertToVO(similarItem, user, currentUserId);
            voList.add(vo);
        }

        return voList;
    }

    private String extractKeyword(String title) {
        if (title == null || title.trim().isEmpty()) {
            return null;
        }

        String lowerTitle = title.toLowerCase();

        for (Map.Entry<String, List<String>> entry : KEYWORD_TAG_MAP.entrySet()) {
            if (lowerTitle.contains(entry.getKey().toLowerCase())) {
                return entry.getKey();
            }
        }

        if (title.length() > 6) {
            return title.substring(0, 6);
        }

        return title;
    }

    private ItemListVO convertToVO(Item item, User user, Long currentUserId) {
        ItemListVO.ItemListVOBuilder builder = ItemListVO.builder()
                .id(item.getId())
                .userId(item.getUserId())
                .title(item.getTitle())
                .coverImage(item.getCoverImage())
                .price(item.getPrice())
                .originalPrice(item.getOriginalPrice())
                .status(item.getStatus())
                .statusName(ItemConstants.getStatusName(item.getStatus()))
                .condition(item.getCondition())
                .conditionName(ItemConstants.getConditionName(item.getCondition()))
                .viewCount(item.getViewCount())
                .likeCount(item.getLikeCount())
                .favoriteCount(item.getFavoriteCount())
                .favorited(currentUserId != null && favoriteService.isFavorited(currentUserId, item.getId()))
                .distance(item.getDistance())
                .location(item.getLocation())
                .createTime(item.getCreateTime());

        if (user != null) {
            builder.nickname(user.getNickname())
                    .avatar(user.getAvatar());
        }

        return builder.build();
    }
}
