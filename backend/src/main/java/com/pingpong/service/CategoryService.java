package com.pingpong.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pingpong.entity.Category;

import java.util.List;

public interface CategoryService extends IService<Category> {
    
    List<Category> getCategoryList(Integer level);
    
    List<Category> getCategoryTree();
    
    boolean addCategory(Category category);
    
    boolean updateCategory(Category category);
    
    boolean deleteCategory(Long id);
}
