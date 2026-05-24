package com.community.idle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.community.idle.constants.StoryConstants;
import com.community.idle.dto.StoryPublishDTO;
import com.community.idle.entity.Story;
import com.community.idle.entity.User;
import com.community.idle.exception.BusinessException;
import com.community.idle.mapper.StoryMapper;
import com.community.idle.mapper.UserMapper;
import com.community.idle.utils.RedisUtil;
import com.community.idle.vo.StoryVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StoryService {

    private final StoryMapper storyMapper;
    private final UserMapper userMapper;
    private final RedisUtil redisUtil;

    public StoryService(StoryMapper storyMapper, UserMapper userMapper, RedisUtil redisUtil) {
        this.storyMapper = storyMapper;
        this.userMapper = userMapper;
        this.redisUtil = redisUtil;
    }

    @Transactional(rollbackFor = Exception.class)
    public Story publishStory(Long userId, StoryPublishDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() != 1) {
            throw new BusinessException("用户状态异常");
        }

        Story story = new Story();
        story.setUserId(userId);
        story.setContent(dto.getContent());
        story.setImages(dto.getImages());
        story.setLikeCount(0);
        story.setViewCount(0);
        story.setCommentCount(0);
        story.setStatus(StoryConstants.STATUS_NORMAL);

        storyMapper.insert(story);
        return story;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteStory(Long userId, Long storyId) {
        Story story = storyMapper.selectById(storyId);
        if (story == null) {
            throw new BusinessException("故事不存在");
        }
        if (!story.getUserId().equals(userId)) {
            throw new BusinessException("无权限删除");
        }

        storyMapper.deleteById(storyId);

        String likeKey = StoryConstants.LIKE_KEY_PREFIX + storyId;
        redisUtil.del(likeKey);
    }

    public List<StoryVO> getMyStories(Long userId, Long pageNum, Long pageSize) {
        Long offset = (pageNum - 1) * pageSize;
        List<StoryVO> stories = storyMapper.selectMyStories(userId, offset, pageSize);

        for (StoryVO vo : stories) {
            enrichStoryVO(vo, userId);
        }

        return stories;
    }

    public boolean toggleLike(Long userId, Long storyId) {
        Story story = storyMapper.selectById(storyId);
        if (story == null || story.getStatus() != StoryConstants.STATUS_NORMAL) {
            throw new BusinessException("故事不存在或已删除");
        }

        String likeKey = StoryConstants.LIKE_KEY_PREFIX + storyId;
        Boolean isMember = redisUtil.sHasKey(likeKey, userId.toString());

        if (Boolean.TRUE.equals(isMember)) {
            redisUtil.sRemove(likeKey, userId.toString());
            storyMapper.update(null, new LambdaUpdateWrapper<Story>()
                    .eq(Story::getId, storyId)
                    .setSql("like_count = like_count - 1"));
            return false;
        } else {
            redisUtil.sSet(likeKey, userId.toString());
            storyMapper.update(null, new LambdaUpdateWrapper<Story>()
                    .eq(Story::getId, storyId)
                    .setSql("like_count = like_count + 1"));
            return true;
        }
    }

    public StoryVO getStoryDetail(Long storyId, Long currentUserId) {
        StoryVO vo = storyMapper.selectStoryDetail(storyId, currentUserId);
        if (vo == null) {
            throw new BusinessException("故事不存在");
        }

        enrichStoryVO(vo, currentUserId);

        storyMapper.update(null, new LambdaUpdateWrapper<Story>()
                .eq(Story::getId, storyId)
                .setSql("view_count = view_count + 1"));

        return vo;
    }

    private void enrichStoryVO(StoryVO vo, Long currentUserId) {
        if (vo.getImages() != null && !vo.getImages().isEmpty()) {
            List<String> imageList = Arrays.stream(vo.getImages().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            vo.setImages(imageList);
        }

        if (currentUserId != null) {
            String likeKey = StoryConstants.LIKE_KEY_PREFIX + vo.getId();
            Boolean liked = redisUtil.sHasKey(likeKey, currentUserId.toString());
            vo.setLiked(Boolean.TRUE.equals(liked));
        } else {
            vo.setLiked(false);
        }
    }

    public long getLikeCount(Long storyId) {
        String likeKey = StoryConstants.LIKE_KEY_PREFIX + storyId;
        return redisUtil.sSize(likeKey);
    }

    public boolean isLiked(Long userId, Long storyId) {
        String likeKey = StoryConstants.LIKE_KEY_PREFIX + storyId;
        return Boolean.TRUE.equals(redisUtil.sHasKey(likeKey, userId.toString()));
    }
}
