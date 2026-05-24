package com.community.idle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String openid;

    private String sessionKey;

    private String nickname;

    private String avatar;

    private Integer gender;

    private String phone;

    private Integer status;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private String location;

    private LocalDateTime cancelApplyTime;

    private String cancelReason;

    private LocalDateTime cancelProcessTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
