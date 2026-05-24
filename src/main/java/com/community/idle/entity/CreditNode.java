package com.community.idle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("credit_node")
public class CreditNode implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String nodeCode;

    private String nodeName;

    private String nodeDesc;

    private String icon;

    private Integer requiredScore;

    private Integer requiredDays;

    private Integer requiredTradeCount;

    private Integer requireVerification;

    private Integer sortOrder;

    private Integer status;
}
