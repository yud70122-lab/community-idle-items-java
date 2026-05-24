package com.community.idle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("visitor_record")
public class VisitorRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long visitorId;

    private Long itemId;

    private Integer visitType;

    private String visitorNickname;

    private String visitorAvatar;

    private Integer visitorCreditLevel;

    private LocalDateTime visitTime;

    @TableLogic
    private Integer deleted;
}
