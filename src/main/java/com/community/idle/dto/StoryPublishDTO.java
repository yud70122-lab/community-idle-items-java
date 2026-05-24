package com.community.idle.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel("发布故事请求")
public class StoryPublishDTO {

    @NotBlank(message = "故事内容不能为空")
    @Length(max = 500, message = "故事内容不能超过500字")
    @ApiModelProperty(value = "故事内容", required = true, example = "今天淘到了一个好宝贝！")
    private String content;

    @ApiModelProperty(value = "图片URL，多张逗号分隔", example = "img1.jpg,img2.jpg")
    private String images;
}
