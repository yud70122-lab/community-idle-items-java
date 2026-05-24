package com.community.idle.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel("登录请求参数")
public class LoginDTO {

    @NotBlank(message = "code不能为空")
    @ApiModelProperty(value = "微信登录code", required = true, example = "023i2kFa154h4D0V6QJa1Z2234")
    private String code;

    @ApiModelProperty("用户昵称")
    private String nickname;

    @ApiModelProperty("头像URL")
    private String avatar;

    @ApiModelProperty("性别：0-未知，1-男，2-女")
    private Integer gender;
}
