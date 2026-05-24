package com.community.idle.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("故事信息")
public class StoryVO {

    @ApiModelProperty("故事ID")
    private Long id;

    @ApiModelProperty("发布用户ID")
    private Long userId;

    @ApiModelProperty("发布用户昵称")
    private String nickname;

    @ApiModelProperty("发布用户头像")
    private String avatar;

    @ApiModelProperty("故事内容")
    private String content;

    @ApiModelProperty("图片列表")
    private List<String> images;

    @ApiModelProperty("点赞数")
    private Integer likeCount;

    @ApiModelProperty("浏览数")
    private Integer viewCount;

    @ApiModelProperty("评论数")
    private Integer commentCount;

    @ApiModelProperty("当前用户是否已点赞")
    private Boolean liked;

    @ApiModelProperty("发布时间")
    private LocalDateTime createTime;
}
