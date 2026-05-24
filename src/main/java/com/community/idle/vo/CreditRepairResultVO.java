package com.community.idle.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("信用修复申请结果")
public class CreditRepairResultVO {

    @ApiModelProperty("修复工单号")
    private String orderNo;

    @ApiModelProperty("工单状态：1-审核中，2-已通过，3-已拒绝，4-已取消")
    private Integer status;

    @ApiModelProperty("状态名称")
    private String statusName;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;
}
