package com.campus.secondhand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.secondhand.common.RedisKeyConstants;
import com.campus.secondhand.entity.Category;
import com.campus.secondhand.mapper.CategoryMapper;
import com.campus.secondhand.service.CategoryService;
import com.campus.secondhand.util.RedisCacheUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Autowired
    private RedisCacheUtil cacheUtil;

    @Override
    public List<Category> listAll() {
        // 先按父分类聚合（一级分类在前），再按排序值、ID 稳定排序，避免后台分类列表乱序
        return list(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getParentId)
                .orderByAsc(Category::getSort)
                .orderByAsc(Category::getId));
    }

    @Override
    public List<Category> listMainCategories() {
        return list(new LambdaQueryWrapper<Category>()
                .isNull(Category::getParentId)
                .orderByAsc(Category::getSort));
    }

    @Override
    public List<Category> listSubCategories(Long parentId) {
        return list(new LambdaQueryWrapper<Category>()
                .eq(Category::getParentId, parentId)
                .orderByAsc(Category::getSort));
    }

    @Override
    public List<Category> getCategoryTree() {
        List<Category> tree = cacheUtil.get(RedisKeyConstants.CACHE_CATEGORY_TREE);
        if (tree != null) return tree;
        tree = buildTreeInternal();
        cacheUtil.set(RedisKeyConstants.CACHE_CATEGORY_TREE, tree, RedisKeyConstants.TTL_CATEGORY);
        return tree;
    }

    private List<Category> buildTreeInternal() {
        List<Category> all = listAll();
        List<Category> mains = all.stream()
                .filter(c -> c.getParentId() == null)
                .collect(Collectors.toList());
        for (Category main : mains) {
            List<Category> subs = all.stream()
                    .filter(c -> main.getId().equals(c.getParentId()))
                    .collect(Collectors.toList());
            main.setChildren(subs);
        }
        return mains;
    }

    // ===== 缓存失效：分类写操作后自动清除缓存 =====

    @Override
    public boolean save(Category entity) {
        boolean ok = super.save(entity);
        if (ok) evictCategoryCache();
        return ok;
    }

    @Override
    public boolean updateById(Category entity) {
        boolean ok = super.updateById(entity);
        if (ok) evictCategoryCache();
        return ok;
    }

    @Override
    public boolean removeById(Serializable id) {
        boolean ok = super.removeById(id);
        if (ok) evictCategoryCache();
        return ok;
    }

    private void evictCategoryCache() {
        cacheUtil.delete(RedisKeyConstants.CACHE_CATEGORY_TREE);
    }
}
