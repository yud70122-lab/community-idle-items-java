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
@ApiModel("信用成长树返回结果")
public class CreditTreeVO {

    @ApiModelProperty("用户ID")
    private Long userId;

    @ApiModelProperty("信用分")
    private Integer creditScore;

    @ApiModelProperty("信用等级")
    private Integer creditLevel;

    @ApiModelProperty("信用等级名称")
    private String creditLevelName;

    @ApiModelProperty("是否实名认证")
    private Boolean identityVerified;

    @ApiModelProperty("是否手机认证")
    private Boolean phoneVerified;

    @ApiModelProperty("首次交换时间")
    private LocalDateTime firstTradeTime;

    @ApiModelProperty("连续履约天数")
    private Integer continuousFulfillDays;

    @ApiModelProperty("历史最高连续履约天数")
    private Integer maxContinuousFulfillDays;

    @ApiModelProperty("累计交易次数")
    private Integer totalTradeCount;

    @ApiModelProperty("累计履约次数")
    private Integer totalFulfillCount;

    @ApiModelProperty("待处理违约记录数")
    private Integer pendingViolationCount;

    @ApiModelProperty("成长节点列表")
    private List<CreditNodeVO> nodes;
}
