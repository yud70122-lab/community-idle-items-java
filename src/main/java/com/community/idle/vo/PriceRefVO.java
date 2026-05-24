package com.community.idle.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("价格参考结果")
public class PriceRefVO {

    @ApiModelProperty(value = "最低价", example = "50.00")
    private BigDecimal minPrice;

    @ApiModelProperty(value = "最高价", example = "80.00")
    private BigDecimal maxPrice;

    @ApiModelProperty(value = "平均价", example = "65.00")
    private BigDecimal avgPrice;

    @ApiModelProperty(value = "参考价格区间", example = "50-80元")
    private String referenceRange;

    @ApiModelProperty(value = "历史成交数量", example = "12")
    private Integer tradeCount;
}
