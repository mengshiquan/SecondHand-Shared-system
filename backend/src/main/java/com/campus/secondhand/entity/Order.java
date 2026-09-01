package com.campus.secondhand.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体
 */
@Data
@TableName("t_order")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单编号 */
    private String orderNo;

    /** 商品ID */
    private Long productId;

    /** 买家ID */
    private Long buyerId;

    /** 卖家ID */
    private Long sellerId;

    /** 成交价格 */
    private BigDecimal price;

    /** 收货人姓名 */
    private String buyerName;

    /** 联系电话 */
    private String buyerPhone;

    /** 收货地址 */
    private String buyerAddress;

    /** 预留过期时间（待付款30分钟后） */
    private LocalDateTime expireTime;

    /**
     * 订单状态：
     * PENDING-待付款 PAID-已付款 SHIPPED-已发货 COMPLETED-已完成 CANCELLED-已取消
     */
    private String status;

    /** 备注 */
    private String remark;

    /** 收货地址ID */
    private Long addressId;

    /** 退款状态 */
    private String refundStatus;

    /** 退款原因 */
    private String refundReason;

    /** 退款申请时间 */
    private LocalDateTime refundTime;

    /** 付款时间 */
    private LocalDateTime paymentTime;

    /** 支付渠道：ALIPAY-支付宝沙箱 WECHAT-微信模拟 */
    private String payChannel;

    /** 第三方支付交易号 */
    private String payTradeNo;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
