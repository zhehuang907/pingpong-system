package com.pingpong.controller;

import com.pingpong.common.Result;
import com.pingpong.entity.Category;
import com.pingpong.entity.Tag;
import com.pingpong.service.CategoryService;
import com.pingpong.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/list")
    public Result<List<Category>> getCategoryList(@RequestParam(required = false) Integer level) {
        List<Category> list = categoryService.getCategoryList(level);
        return Result.success(list);
    }

    @GetMapping("/tree")
    public Result<List<Category>> getCategoryTree() {
        List<Category> tree = categoryService.getCategoryTree();
        return Result.success(tree);
    }

    @PostMapping
    public Result<Boolean> addCategory(@RequestBody Category category) {
        boolean result = categoryService.addCategory(category);
        return Result.success(result);
    }

    @PutMapping
    public Result<Boolean> updateCategory(@RequestBody Category category) {
        boolean result = categoryService.updateCategory(category);
        return Result.success(result);
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> deleteCategory(@PathVariable Long id) {
        boolean result = categoryService.deleteCategory(id);
        return Result.success(result);
    }
}
