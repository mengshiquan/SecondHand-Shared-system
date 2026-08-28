package com.campus.secondhand.dto;

import lombok.Data;

/**
 * 商品查询 DTO
 */
@Data
public class ProductQueryDTO {

    private String keyword;

    private Long categoryId;

    /** 父分类ID（一级分类），查询该分类下所有子分类商品 */
    private Long parentCategoryId;

    private String status;

    /** 当前登录用户ID（用于收藏状态） */
    private Long currentUserId;

    /** 发布者ID（我的发布） */
    private Long sellerId;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
