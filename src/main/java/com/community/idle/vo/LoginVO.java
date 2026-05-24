package com.community.idle.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("登录响应结果")
public class LoginVO {

    @ApiModelProperty("登录令牌")
    private String token;

    @ApiModelProperty("用户ID")
    private Long userId;

    @ApiModelProperty("微信openid")
    private String openid;

    @ApiModelProperty("用户昵称")
    private String nickname;

    @ApiModelProperty("头像URL")
    private String avatar;
}
