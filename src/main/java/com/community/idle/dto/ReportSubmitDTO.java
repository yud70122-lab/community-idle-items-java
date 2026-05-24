package com.community.idle.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@ApiModel("提交举报请求")
public class ReportSubmitDTO {

    @NotNull(message = "举报类型不能为空")
    @ApiModelProperty(value = "举报类型：1-物品，2-用户，3-评论", required = true, example = "1")
    private Integer reportType;

    @NotNull(message = "举报目标ID不能为空")
    @ApiModelProperty(value = "举报目标ID（物品ID/用户ID/评论ID）", required = true, example = "1")
    private Long targetId;

    @NotNull(message = "举报原因不能为空")
    @ApiModelProperty(value = "举报原因：1-虚假信息，2-违禁品，3-诈骗行为，4-不当内容，5-侵权问题，6-其他问题", required = true, example = "1")
    private Integer reason;

    @Length(max = 500, message = "描述不能超过500字")
    @ApiModelProperty(value = "详细描述", example = "物品信息与实际不符，存在虚假宣传")
    private String description;

    @ApiModelProperty(value = "举报图片URL列表", example = "[\"img1.jpg\", \"img2.jpg\"]")
    private List<String> images;
}
