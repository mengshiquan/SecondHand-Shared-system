package com.campus.secondhand.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品分类实体
 */
@Data
@TableName("t_category")
public class Category {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类名称 */
    private String name;

    /** 父级分类ID，NULL为一级分类 */
    private Long parentId;

    /** 分类图标 */
    private String icon;

    /** 子分类列表（仅用于 API 返回，非数据库字段） */
    @TableField(exist = false)
    private java.util.List<Category> children;

    /** 排序 */
    private Integer sort;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
