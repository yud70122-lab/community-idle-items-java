package com.community.idle.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@ApiModel("编辑物品请求")
public class ItemEditDTO {

    @NotBlank(message = "物品标题不能为空")
    @Length(max = 100, message = "物品标题不能超过100字")
    @ApiModelProperty(value = "物品标题", required = true, example = "九成新小米手机")
    private String title;

    @NotBlank(message = "物品描述不能为空")
    @Length(max = 2000, message = "物品描述不能超过2000字")
    @ApiModelProperty(value = "物品描述", required = true, example = "自用小米手机，九成新，无磕碰")
    private String description;

    @NotNull(message = "分类ID不能为空")
    @ApiModelProperty(value = "分类ID", required = true, example = "1")
    private Long categoryId;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0", message = "价格不能小于0")
    @ApiModelProperty(value = "价格", required = true, example = "99.99")
    private BigDecimal price;

    @ApiModelProperty(value = "原价", example = "299.00")
    private BigDecimal originalPrice;

    @ApiModelProperty(value = "封面图片URL", required = true, example = "cover.jpg")
    private String coverImage;

    @ApiModelProperty(value = "图片URL列表，多张用逗号分隔", example = "img1.jpg,img2.jpg,img3.jpg")
    private String images;

    @NotNull(message = "新旧程度不能为空")
    @Min(value = 1, message = "新旧程度值无效")
    @Max(value = 5, message = "新旧程度值无效")
    @ApiModelProperty(value = "新旧程度：1-全新，2-几乎全新，3-良好，4-一般，5-较差", required = true, example = "2")
    private Integer condition;

    @NotNull(message = "交易类型不能为空")
    @Min(value = 1, message = "交易类型值无效")
    @Max(value = 3, message = "交易类型值无效")
    @ApiModelProperty(value = "交易类型：1-免费送，2-以物换物，3-出售", required = true, example = "3")
    private Integer tradeType;

    @ApiModelProperty(value = "标签列表", example = "[\"手机\", \"数码\"]")
    private List<String> tags;

    @ApiModelProperty(value = "期望交换物品描述（以物换物时必填）", example = "想换同等价值的平板")
    private String expectExchange;
}
