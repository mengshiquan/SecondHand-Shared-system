package com.campus.secondhand.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单展示 VO
 */
@Data
public class OrderVO {

    private Long id;
    private String orderNo;
    private Long productId;
    private String productTitle;
    private String productImage;
    private Long buyerId;
    private String buyerName;
    private String buyerPhone;
    private String buyerAddress;
    private String buyerNickname;
    private Long sellerId;
    private String sellerName;
    private BigDecimal price;
    private String status;
    private String payChannel;
    private String remark;
    private String refundStatus;
    private String refundReason;
    private LocalDateTime refundTime;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
