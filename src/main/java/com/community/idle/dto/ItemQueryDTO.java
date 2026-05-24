package com.community.idle.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@ApiModel("物品列表查询参数")
public class ItemQueryDTO {

    @ApiModelProperty(value = "物品状态：0-已下架，1-在售，2-已锁定，3-已售出，4-审核中", example = "1")
    private Integer status;

    @ApiModelProperty(value = "分类ID", example = "1")
    private Long categoryId;

    @ApiModelProperty(value = "搜索关键词", example = "手机")
    private String keyword;

    @ApiModelProperty(value = "页码", example = "1")
    private Long pageNum = 1L;

    @ApiModelProperty(value = "每页数量", example = "20")
    private Long pageSize = 20L;

    @ApiModelProperty(value = "排序方式：match-智能匹配（默认）, distance-距离最近, time-最新发布, popularity-最受欢迎", example = "match")
    private String sortBy = "match";

    @ApiModelProperty(value = "当前用户纬度（距离筛选时必填）", example = "31.2304")
    private BigDecimal userLatitude;

    @ApiModelProperty(value = "当前用户经度（距离筛选时必填）", example = "121.4737")
    private BigDecimal userLongitude;

    @ApiModelProperty(value = "距离范围（km）：1, 3, 5, null表示全城", example = "3")
    private BigDecimal distance;

    @ApiModelProperty(value = "成色列表（多选）：1-全新，2-几乎全新，3-良好，4-一般，5-较差", example = "[1, 2, 3]")
    private List<Integer> conditions;

    @ApiModelProperty(value = "交易类型（单选）：1-免费送，2-以物换物，3-出售", example = "3")
    private Integer tradeType;

    @ApiModelProperty(value = "最低价", example = "10")
    private BigDecimal minPrice;

    @ApiModelProperty(value = "最高价", example = "500")
    private BigDecimal maxPrice;

    @ApiModelProperty(value = "当前用户ID（智能排序用）", hidden = true)
    private Long currentUserId;
}
