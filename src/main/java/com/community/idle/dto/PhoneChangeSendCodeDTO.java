package com.community.idle.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
@ApiModel("发送换绑手机号验证码请求")
public class PhoneChangeSendCodeDTO {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @ApiModelProperty(value = "手机号", required = true, example = "13800138000")
    private String phone;

    @NotBlank(message = "验证码类型不能为空")
    @ApiModelProperty(value = "类型：old-旧手机号验证，new-新手机号验证", required = true, example = "old")
    private String type;
}
