package com.campus.secondhand.controller;

import com.campus.secondhand.common.Result;
import com.campus.secondhand.entity.Category;
import com.campus.secondhand.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类模块接口
 */
@RestController
@RequestMapping("/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /** 获取所有分类（扁平列表） */
    @GetMapping("/list")
    public Result<List<Category>> list() {
        return Result.success(categoryService.listAll());
    }

    /** 获取一级分类列表 */
    @GetMapping("/main")
    public Result<List<Category>> mainCategories() {
        return Result.success(categoryService.listMainCategories());
    }

    /** 获取指定父分类下的子分类 */
    @GetMapping("/sub/{parentId}")
    public Result<List<Category>> subCategories(@PathVariable Long parentId) {
        return Result.success(categoryService.listSubCategories(parentId));
    }

    /** 获取完整分类树（一级嵌套二级） */
    @GetMapping("/tree")
    public Result<List<Category>> tree() {
        return Result.success(categoryService.getCategoryTree());
    }
}
