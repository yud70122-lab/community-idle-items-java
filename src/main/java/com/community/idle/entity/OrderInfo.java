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
@TableName("order_info")
public class OrderInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long buyerId;

    private Long sellerId;

    private Long itemId;

    private String itemTitle;

    private String itemImage;

    private BigDecimal price;

    private BigDecimal shippingFee;

    private BigDecimal totalAmount;

    private Integer paymentMethod;

    private LocalDateTime paymentTime;

    private Integer tradeType;

    private Long addressId;

    private String addressSnapshot;

    private Integer status;

    private String buyerRemark;

    private String sellerRemark;

    private String cancelReason;

    private LocalDateTime cancelTime;

    private LocalDateTime shipTime;

    private LocalDateTime receiveTime;

    private LocalDateTime completeTime;

    private LocalDateTime expireTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
