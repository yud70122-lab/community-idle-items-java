package com.community.idle.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("物品列表项")
public class ItemListVO {

    @ApiModelProperty("物品ID")
    private Long id;

    @ApiModelProperty("发布用户ID")
    private Long userId;

    @ApiModelProperty("发布用户昵称")
    private String nickname;

    @ApiModelProperty("发布用户头像")
    private String avatar;

    @ApiModelProperty("物品标题")
    private String title;

    @ApiModelProperty("封面图片")
    private String coverImage;

    @ApiModelProperty("价格")
    private BigDecimal price;

    @ApiModelProperty("原价")
    private BigDecimal originalPrice;

    @ApiModelProperty("物品状态：0-已下架，1-在售，2-已锁定，3-已售出")
    private Integer status;

    @ApiModelProperty("状态名称")
    private String statusName;

    @ApiModelProperty("新旧程度")
    private Integer condition;

    @ApiModelProperty("新旧程度名称")
    private String conditionName;

    @ApiModelProperty("浏览次数")
    private Integer viewCount;

    @ApiModelProperty("点赞次数")
    private Integer likeCount;

    @ApiModelProperty("收藏次数")
    private Integer favoriteCount;

    @ApiModelProperty("是否已收藏")
    private Boolean favorited;

    @ApiModelProperty("发布时间")
    private LocalDateTime createTime;

    @ApiModelProperty("距离（km）")
    private BigDecimal distance;

    @ApiModelProperty("位置信息")
    private String location;
}
