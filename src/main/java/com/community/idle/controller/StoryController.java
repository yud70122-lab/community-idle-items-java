package com.community.idle.controller;

import com.community.idle.common.Result;
import com.community.idle.dto.StoryPublishDTO;
import com.community.idle.entity.Story;
import com.community.idle.service.StoryService;
import com.community.idle.vo.StoryVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "故事广场")
@RestController
@RequestMapping("/story")
public class StoryController {

    private final StoryService storyService;

    public StoryController(StoryService storyService) {
        this.storyService = storyService;
    }

    @ApiOperation("发布故事")
    @PostMapping("/publish")
    public Result<Map<String, Object>> publishStory(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @Validated @RequestBody StoryPublishDTO dto
    ) {
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        Story story = storyService.publishStory(userId, dto);
        Map<String, Object> data = new HashMap<>();
        data.put("id", story.getId());
        return Result.success("发布成功", data);
    }

    @ApiOperation("删除故事")
    @DeleteMapping("/{id}")
    public Result<Void> deleteStory(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @PathVariable("id") Long storyId
    ) {
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        storyService.deleteStory(userId, storyId);
        return Result.success("删除成功");
    }

    @ApiOperation("查询我的故事列表")
    @GetMapping("/mylist")
    public Result<List<StoryVO>> getMyStories(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @ApiParam("页码") @RequestParam(defaultValue = "1") Long pageNum,
            @ApiParam("每页数量") @RequestParam(defaultValue = "10") Long pageSize
    ) {
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        List<StoryVO> stories = storyService.getMyStories(userId, pageNum, pageSize);
        return Result.success("获取成功", stories);
    }

    @ApiOperation("点赞/取消点赞")
    @PostMapping("/like")
    public Result<Map<String, Object>> toggleLike(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @RequestParam("storyId") Long storyId
    ) {
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        boolean liked = storyService.toggleLike(userId, storyId);
        Map<String, Object> data = new HashMap<>();
        data.put("liked", liked);
        data.put("likeCount", storyService.getLikeCount(storyId));
        return Result.success(liked ? "点赞成功" : "取消点赞成功", data);
    }

    @ApiOperation("获取故事详情")
    @GetMapping("/{id}")
    public Result<StoryVO> getStoryDetail(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @PathVariable("id") Long storyId
    ) {
        StoryVO vo = storyService.getStoryDetail(storyId, userId);
        return Result.success("获取成功", vo);
    }
}
