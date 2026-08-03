package com.campus.secondhand.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品展示 VO
 */
@Data
public class ProductVO {

    private Long id;
    private String title;
    private String description;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private List<String> images;
    private Long categoryId;
    private String categoryName;
    private Long userId;
    private String sellerName;
    private String sellerAvatar;
    private String status;
    private Integer viewCount;
    private Boolean favorited;
    private LocalDateTime createTime;
}
