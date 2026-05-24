package com.community.idle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("user_credit")
public class UserCredit implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Integer creditScore;

    private Integer creditLevel;

    private Integer violationCount;

    private Integer pendingViolationCount;

    private Integer continuousFulfillDays;

    private Integer maxContinuousFulfillDays;

    private Integer totalFulfillCount;

    private Integer totalTradeCount;

    private LocalDateTime firstTradeTime;

    private LocalDateTime lastFulfillTime;

    private Integer identityVerified;

    private Integer phoneVerified;

    private Integer emailVerified;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
