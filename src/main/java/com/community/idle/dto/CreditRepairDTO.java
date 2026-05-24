package com.community.idle.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@ApiModel("信用修复申请请求")
public class CreditRepairDTO {

    @NotNull(message = "违约记录ID不能为空")
    @ApiModelProperty(value = "违约记录ID", required = true, example = "1")
    private Long violationId;

    @NotBlank(message = "修复理由不能为空")
    @ApiModelProperty(value = "修复理由", required = true, example = "交易已协商完成")
    private String repairReason;

    @ApiModelProperty(value = "证明图片URL，多张逗号分隔", example = "img1.jpg,img2.jpg")
    private String proveImages;
}
