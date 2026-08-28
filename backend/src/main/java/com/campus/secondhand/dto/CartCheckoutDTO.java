package com.campus.secondhand.dto;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class CartCheckoutDTO {

    @NotEmpty(message = "请选择要结算的商品")
    private List<Long> cartItemIds;

    @NotNull(message = "请选择收货地址")
    private Long addressId;
}
