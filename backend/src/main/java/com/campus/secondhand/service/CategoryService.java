package com.campus.secondhand.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.secondhand.entity.Category;

import java.util.List;

public interface CategoryService extends IService<Category> {

    /** 获取所有启用的一级分类（含子分类） */
    List<Category> listAll();

    /** 获取一级分类（parent_id IS NULL） */
    List<Category> listMainCategories();

    /** 获取指定父分类下的子分类 */
    List<Category> listSubCategories(Long parentId);

    /** 获取完整分类树（一级 → 二级嵌套） */
    List<Category> getCategoryTree();
}
