package com.community.idle.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("物品详情（编辑用）")
public class ItemDetailVO {

    @ApiModelProperty(value = "物品ID", example = "1")
    private Long id;

    @ApiModelProperty(value = "发布用户ID", example = "123")
    private Long userId;

    @ApiModelProperty(value = "物品标题", example = "九成新小米手机")
    private String title;

    @ApiModelProperty(value = "物品描述", example = "自用小米手机，九成新，无磕碰")
    private String description;

    @ApiModelProperty(value = "分类ID", example = "1")
    private Long categoryId;

    @ApiModelProperty(value = "价格", example = "99.99")
    private BigDecimal price;

    @ApiModelProperty(value = "原价", example = "299.00")
    private BigDecimal originalPrice;

    @ApiModelProperty(value = "封面图片URL", example = "cover.jpg")
    private String coverImage;

    @ApiModelProperty(value = "图片URL列表，多张用逗号分隔", example = "img1.jpg,img2.jpg,img3.jpg")
    private String images;

    @ApiModelProperty(value = "新旧程度：1-全新，2-几乎全新，3-良好，4-一般，5-较差", example = "2")
    private Integer condition;

    @ApiModelProperty(value = "交易类型：1-免费送，2-以物换物，3-出售", example = "3")
    private Integer tradeType;

    @ApiModelProperty(value = "物品状态：0-已下架，1-在售，2-已锁定，3-已售出，4-审核中", example = "1")
    private Integer status;

    @ApiModelProperty(value = "标签列表", example = "[\"手机\", \"数码\"]")
    private List<String> tags;

    @ApiModelProperty(value = "期望交换物品描述", example = "想换同等价值的平板")
    private String expectExchange;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;
}
