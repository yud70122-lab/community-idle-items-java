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
@ApiModel("访客信息")
public class VisitorVO {

    @ApiModelProperty("访客ID")
    private Long visitorId;

    @ApiModelProperty("脱敏昵称，如张**")
    private String nickname;

    @ApiModelProperty("头像")
    private String avatar;

    @ApiModelProperty("信用等级")
    private Integer creditLevel;

    @ApiModelProperty("信用等级名称")
    private String creditLevelName;

    @ApiModelProperty("访问的物品ID")
    private Long itemId;

    @ApiModelProperty("访问的物品标题")
    private String itemTitle;

    @ApiModelProperty("访问时间")
    private LocalDateTime visitTime;
}
