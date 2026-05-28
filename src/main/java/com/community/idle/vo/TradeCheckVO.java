package com.community.idle.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("交易前置校验结果")
public class TradeCheckVO {

    @ApiModelProperty("是否可以交易")
    private Boolean canTrade;

    @ApiModelProperty("不可交易原因")
    private String reason;

    @ApiModelProperty("用户信用分")
    private Integer creditScore;

    @ApiModelProperty("用户信用等级")
    private Integer creditLevel;

    @ApiModelProperty("用户信用等级名称")
    private String creditLevelName;

    @ApiModelProperty("最低要求信用分")
    private Integer minRequiredCreditScore;

    @ApiModelProperty("交易类型：1-免费送，2-以物换物，3-出售")
    private Integer tradeType;

    @ApiModelProperty("交易类型名称")
    private String tradeTypeName;

    @ApiModelProperty("物品ID")
    private Long itemId;

    @ApiModelProperty("物品标题")
    private String itemTitle;

    @ApiModelProperty("物品价格")
    private BigDecimal price;

    @ApiModelProperty("运费")
    private BigDecimal shippingFee;

    @ApiModelProperty("卖家ID")
    private Long sellerId;

    @ApiModelProperty("卖家昵称")
    private String sellerNickname;

    @ApiModelProperty("卖家头像")
    private String sellerAvatar;

    @ApiModelProperty("卖家信用分")
    private Integer sellerCreditScore;

    @ApiModelProperty("是否实名认证")
    private Boolean identityVerified;

    @ApiModelProperty("是否手机认证")
    private Boolean phoneVerified;

    @ApiModelProperty("是否有待处理违规")
    private Boolean hasPendingViolation;
}
