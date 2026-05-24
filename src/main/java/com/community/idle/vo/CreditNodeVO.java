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
@ApiModel("成长节点信息")
public class CreditNodeVO {

    @ApiModelProperty("节点ID")
    private Long id;

    @ApiModelProperty("节点编码")
    private String nodeCode;

    @ApiModelProperty("节点名称")
    private String nodeName;

    @ApiModelProperty("节点描述")
    private String nodeDesc;

    @ApiModelProperty("节点图标")
    private String icon;

    @ApiModelProperty("解锁状态：0-未解锁，1-已解锁，2-进行中")
    private Integer status;

    @ApiModelProperty("解锁状态名称")
    private String statusName;

    @ApiModelProperty("当前进度值")
    private Integer currentValue;

    @ApiModelProperty("目标进度值")
    private Integer targetValue;

    @ApiModelProperty("完成进度百分比")
    private Double progress;

    @ApiModelProperty("排序")
    private Integer sortOrder;
}
