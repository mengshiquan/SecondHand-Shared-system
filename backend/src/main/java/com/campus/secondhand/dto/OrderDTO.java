package com.campus.secondhand.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 创建订单 DTO
 */
@Data
public class OrderDTO {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotBlank(message = "收货人姓名不能为空")
    private String buyerName;

    @NotBlank(message = "联系电话不能为空")
    private String buyerPhone;

    @NotBlank(message = "收货地址不能为空")
    private String buyerAddress;

    private String remark;

    /** 收货地址ID（购物车结算时必传） */
    private Long addressId;
}
