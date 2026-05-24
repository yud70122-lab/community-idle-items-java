package com.community.idle.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("注销申请请求")
public class UserCancelDTO {

    @ApiModelProperty(value = "注销原因", example = "不再使用该账号")
    private String reason;
}
