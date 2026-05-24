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
@ApiModel("生成描述结果")
public class GenDescVO {

    @ApiModelProperty(value = "生成的物品描述", example = "自用九成新小米手机，功能完好无磕碰，搭载最新系统，拍照清晰，续航持久。适合学生党和上班族使用，性价比高，支持验货面交。")
    private String description;
}
