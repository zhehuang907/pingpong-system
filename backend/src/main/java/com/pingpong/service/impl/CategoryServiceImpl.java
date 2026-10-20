package com.pingpong.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pingpong.entity.Category;
import com.pingpong.mapper.CategoryMapper;
import com.pingpong.service.CategoryService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Override
    @Cacheable(value = "category:list", key = "T(java.util.Arrays).toString(#root.args)")
    public List<Category> getCategoryList(Integer level) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getStatus, 1);
        if (level != null) {
            wrapper.eq(Category::getLevel, level);
        }
        wrapper.orderByAsc(Category::getSort);
        return this.list(wrapper);
    }

    @Override
    public List<Category> getCategoryTree() {
        List<Category> allCategories = this.list(new LambdaQueryWrapper<Category>()
            .eq(Category::getStatus, 1)
            .orderByAsc(Category::getSort));

        List<Category> rootCategories = allCategories.stream()
            .filter(c -> c.getParentId() == 0 || c.getParentId() == null)
            .collect(Collectors.toList());

        for (Category root : rootCategories) {
            List<Category> children = allCategories.stream()
                .filter(c -> root.getId().equals(c.getParentId()))
                .collect(Collectors.toList());
            if (!children.isEmpty()) {
                root.setIcon("has-children");
            }
        }

        return rootCategories;
    }

    @Override
    @Transactional
    public boolean addCategory(Category category) {
        if (category.getLevel() == null) {
            category.setLevel(1);
        }
        if (category.getSort() == null) {
            category.setSort(0);
        }
        if (category.getStatus() == null) {
            category.setStatus(1);
        }
        return this.save(category);
    }

    @Override
    @Transactional
    public boolean updateCategory(Category category) {
        return this.updateById(category);
    }

    @Override
    @Transactional
    public boolean deleteCategory(Long id) {
        List<Category> children = this.list(new LambdaQueryWrapper<Category>()
            .eq(Category::getParentId, id));
        
        if (!children.isEmpty()) {
            for (Category child : children) {
                this.removeById(child.getId());
            }
        }
        
        return this.removeById(id);
    }
}
