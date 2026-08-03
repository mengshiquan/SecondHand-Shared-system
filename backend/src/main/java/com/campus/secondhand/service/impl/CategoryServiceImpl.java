package com.campus.secondhand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.secondhand.entity.Category;
import com.campus.secondhand.mapper.CategoryMapper;
import com.campus.secondhand.service.CategoryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Override
    public List<Category> listAll() {
        return list(new LambdaQueryWrapper<Category>().orderByAsc(Category::getSort));
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
}
