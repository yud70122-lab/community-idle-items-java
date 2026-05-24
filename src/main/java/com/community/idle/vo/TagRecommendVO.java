package com.community.idle.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("智能标签推荐结果")
public class TagRecommendVO {

    @ApiModelProperty(value = "推荐标签列表", example = "[\"手机\", \"数码\", \"九成新\"]")
    private List<String> tags;
}
