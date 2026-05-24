package com.community.idle.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.idle.entity.Story;
import com.community.idle.vo.StoryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StoryMapper extends BaseMapper<Story> {

    List<StoryVO> selectMyStories(@Param("userId") Long userId, @Param("offset") Long offset, @Param("limit") Long limit);

    StoryVO selectStoryDetail(@Param("id") Long id, @Param("currentUserId") Long currentUserId);
}
