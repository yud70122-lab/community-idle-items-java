package com.community.idle.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.idle.common.Result;
import com.community.idle.constants.ItemConstants;
import com.community.idle.dto.ItemEditDTO;
import com.community.idle.dto.ItemPublishDTO;
import com.community.idle.dto.ItemQueryDTO;
import com.community.idle.entity.Item;
import com.community.idle.mapper.ItemMapper;
import com.community.idle.service.ItemService;
import com.community.idle.service.ViewCountService;
import com.community.idle.service.VisitorService;
import com.community.idle.vo.GenDescVO;
import com.community.idle.vo.ItemDetailVO;
import com.community.idle.vo.ItemListVO;
import com.community.idle.vo.PriceRefVO;
import com.community.idle.vo.TagRecommendVO;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "物品管理")
@RestController
@RequestMapping("/item")
public class ItemController {

    private final ItemMapper itemMapper;
    private final ItemService itemService;
    private final VisitorService visitorService;
    private final ViewCountService viewCountService;

    public ItemController(ItemMapper itemMapper, ItemService itemService, VisitorService visitorService,
                          ViewCountService viewCountService) {
        this.itemMapper = itemMapper;
        this.itemService = itemService;
        this.visitorService = visitorService;
        this.viewCountService = viewCountService;
    }

    @ApiOperation("发布物品")
    @PostMapping("/publish")
    public Result<Map<String, Object>> publishItem(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @Validated @RequestBody ItemPublishDTO dto
    ) {
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        Item item = itemService.publishItem(userId, dto);
        Map<String, Object> data = new HashMap<>();
        data.put("id", item.getId());
        data.put("status", item.getStatus());
        data.put("statusName", item.getStatus() == 4 ? "审核中" : "已上架");
        return Result.success("发布成功", data);
    }

    @ApiOperation("获取物品列表（Tab切换）")
    @GetMapping("/list")
    public Result<Page<ItemListVO>> getItemList(
            @ApiIgnore @AuthenticationPrincipal Long currentUserId,
            @ApiParam("物品状态：0-已下架，1-在售，2-已锁定，3-已售出，4-审核中") @RequestParam(required = false) Integer status,
            @ApiParam("分类ID") @RequestParam(required = false) Long categoryId,
            @ApiParam("搜索关键词") @RequestParam(required = false) String keyword,
            @ApiParam("页码") @RequestParam(defaultValue = "1") Long pageNum,
            @ApiParam("每页数量") @RequestParam(defaultValue = "20") Long pageSize,
            @ApiParam("排序方式：match-智能匹配（默认）, distance-距离最近, time-最新发布, popularity-最受欢迎") @RequestParam(defaultValue = "match") String sortBy,
            @ApiParam("当前用户纬度（距离筛选时必填）") @RequestParam(required = false) BigDecimal userLatitude,
            @ApiParam("当前用户经度（距离筛选时必填）") @RequestParam(required = false) BigDecimal userLongitude,
            @ApiParam("距离范围（km）：1, 3, 5, null表示全城") @RequestParam(required = false) BigDecimal distance,
            @ApiParam("成色列表（多选）：1-全新，2-几乎全新，3-良好，4-一般，5-较差") @RequestParam(required = false) List<Integer> conditions,
            @ApiParam("交易类型（单选）：1-免费送，2-以物换物，3-出售") @RequestParam(required = false) Integer tradeType,
            @ApiParam("最低价") @RequestParam(required = false) BigDecimal minPrice,
            @ApiParam("最高价") @RequestParam(required = false) BigDecimal maxPrice
    ) {
        ItemQueryDTO query = new ItemQueryDTO();
        query.setStatus(status);
        query.setCategoryId(categoryId);
        query.setKeyword(keyword);
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        query.setSortBy(sortBy);
        query.setUserLatitude(userLatitude);
        query.setUserLongitude(userLongitude);
        query.setDistance(distance);
        query.setConditions(conditions);
        query.setTradeType(tradeType);
        query.setMinPrice(minPrice);
        query.setMaxPrice(maxPrice);
        query.setCurrentUserId(currentUserId);

        Page<ItemListVO> page = itemService.getItemList(query);
        return Result.success("获取成功", page);
    }

    @ApiOperation("获取我的物品列表")
    @GetMapping("/mylist")
    public Result<Page<ItemListVO>> getMyItems(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @ApiParam("物品状态：0-已下架，1-在售，2-已锁定，3-已售出，4-审核中") @RequestParam(required = false) Integer status,
            @ApiParam("页码") @RequestParam(defaultValue = "1") Long pageNum,
            @ApiParam("每页数量") @RequestParam(defaultValue = "10") Long pageSize
    ) {
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        Page<ItemListVO> page = itemService.getMyItems(userId, status, pageNum, pageSize);
        return Result.success("获取成功", page);
    }

    @ApiOperation("点赞/取消点赞")
    @PostMapping("/like")
    public Result<Map<String, Object>> toggleLike(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @RequestParam("itemId") Long itemId
    ) {
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        boolean liked = itemService.toggleLike(userId, itemId);
        Map<String, Object> data = new HashMap<>();
        data.put("liked", liked);
        Item item = itemMapper.selectById(itemId);
        data.put("likeCount", item != null ? item.getLikeCount() : 0);
        return Result.success(liked ? "点赞成功" : "取消点赞成功", data);
    }

    @ApiOperation("获取物品详情")
    @GetMapping("/{id}")
    public Result<Item> getItemDetail(
            @AuthenticationPrincipal Long visitorId,
            @PathVariable("id") Long itemId
    ) {
        Item item = itemMapper.selectById(itemId);
        if (item == null) {
            return Result.error("物品不存在");
        }

        if (visitorId != null && !visitorId.equals(item.getUserId())) {
            visitorService.recordVisitor(item.getUserId(), visitorId, itemId);
        }

        return Result.success("获取成功", item);
    }

    @ApiOperation("增加物品浏览量")
    @PostMapping("/{id}/view")
    public Result<Map<String, Object>> incrementViewCount(
            @ApiParam("物品ID", required = true) @PathVariable("id") Long itemId
    ) {
        viewCountService.incrementViewCount(itemId);
        int viewCount = viewCountService.getCurrentViewCount(itemId);
        Map<String, Object> data = new HashMap<>();
        data.put("itemId", itemId);
        data.put("viewCount", viewCount);
        return Result.success("浏览量已增加", data);
    }

    @ApiOperation("智能标签推荐")
    @GetMapping("/recommend-tags")
    public Result<TagRecommendVO> recommendTags(
            @ApiParam("物品标题") @RequestParam(required = false) String title,
            @ApiParam("物品描述") @RequestParam(required = false) String description
    ) {
        TagRecommendVO vo = itemService.recommendTags(title, description);
        return Result.success("推荐成功", vo);
    }

    @ApiOperation("价格智能参考")
    @GetMapping("/price-ref")
    public Result<PriceRefVO> getPriceReference(
            @ApiParam("物品名称", required = true) @RequestParam String itemName
    ) {
        PriceRefVO vo = itemService.getPriceReference(itemName);
        return Result.success("获取成功", vo);
    }

    @ApiOperation("一键生成描述")
    @GetMapping("/gen-desc")
    public Result<GenDescVO> generateDescription(
            @ApiParam("品类ID", required = true) @RequestParam Long categoryId,
            @ApiParam("成色：1-全新，2-几乎全新，3-良好，4-一般，5-较差", required = true) @RequestParam Integer condition,
            @ApiParam("卖点列表") @RequestParam(required = false) List<String> sellingPoints
    ) {
        GenDescVO vo = itemService.generateDescription(categoryId, condition, sellingPoints);
        return Result.success("生成成功", vo);
    }

    @ApiOperation("下架物品")
    @PutMapping("/{id}/offline")
    public Result<Map<String, Object>> offlineItem(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @ApiParam("物品ID", required = true) @PathVariable("id") Long itemId
    ) {
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        itemService.offlineItem(userId, itemId);
        Map<String, Object> data = new HashMap<>();
        data.put("id", itemId);
        data.put("status", ItemConstants.STATUS_OFFLINE);
        data.put("statusName", "已下架");
        return Result.success("下架成功", data);
    }

    @ApiOperation("获取物品详情（编辑用）")
    @GetMapping("/{id}/detail")
    public Result<ItemDetailVO> getItemDetailForEdit(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @ApiParam("物品ID", required = true) @PathVariable("id") Long itemId
    ) {
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        ItemDetailVO vo = itemService.getItemDetailForEdit(userId, itemId);
        return Result.success("获取成功", vo);
    }

    @ApiOperation("更新物品")
    @PutMapping("/{id}")
    public Result<Map<String, Object>> updateItem(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @ApiParam("物品ID", required = true) @PathVariable("id") Long itemId,
            @Validated @RequestBody ItemEditDTO dto
    ) {
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        Item item = itemService.updateItem(userId, itemId, dto);
        Map<String, Object> data = new HashMap<>();
        data.put("id", item.getId());
        data.put("status", item.getStatus());
        data.put("statusName", item.getStatus() == ItemConstants.STATUS_REVIEW ? "审核中" : "已上架");
        return Result.success("更新成功", data);
    }

    @ApiOperation("相似物品推荐")
    @GetMapping("/recommend/{id}")
    public Result<List<ItemListVO>> getSimilarItems(
            @AuthenticationPrincipal Long currentUserId,
            @ApiParam("物品ID", required = true) @PathVariable("id") Long itemId,
            @ApiParam("推荐数量") @RequestParam(defaultValue = "10") Integer limit
    ) {
        List<ItemListVO> similarItems = itemService.getSimilarItems(itemId, currentUserId, limit);
        return Result.success("获取成功", similarItems);
    }
}
