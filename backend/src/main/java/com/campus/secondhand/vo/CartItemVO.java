package com.campus.secondhand.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购物车列表项（含商品摘要与失效标记）
 */
@Data
public class CartItemVO {

    private Long id;

    private Long productId;

    private String title;

    private BigDecimal price;

    private java.util.List<String> images;

    private String categoryName;

    private String sellerNickname;

    /** 是否失效（商品已下架/已售出） */
    private Boolean invalid;

    private LocalDateTime createTime;
}
