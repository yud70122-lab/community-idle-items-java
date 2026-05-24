package com.community.idle.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
@ApiModel("换绑手机号请求")
public class PhoneChangeDTO {

    @NotBlank(message = "旧手机号验证码不能为空")
    @ApiModelProperty(value = "旧手机号验证码", required = true, example = "123456")
    private String oldCode;

    @NotBlank(message = "新手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "新手机号格式不正确")
    @ApiModelProperty(value = "新手机号", required = true, example = "13900139000")
    private String newPhone;

    @NotBlank(message = "新手机号验证码不能为空")
    @ApiModelProperty(value = "新手机号验证码", required = true, example = "654321")
    private String newCode;
}
