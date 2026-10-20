package com.pingpong.controller;

import com.pingpong.common.Result;
import com.pingpong.entity.Tag;
import com.pingpong.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tag")
public class TagController {

    @Autowired
    private TagService tagService;

    @GetMapping("/list")
    public Result<List<Tag>> getTagList(@RequestParam(required = false) String category) {
        List<Tag> list = tagService.getTagList(category);
        return Result.success(list);
    }

    @PostMapping
    public Result<Boolean> addTag(@RequestBody Tag tag) {
        boolean result = tagService.addTag(tag);
        return Result.success(result);
    }

    @PutMapping
    public Result<Boolean> updateTag(@RequestBody Tag tag) {
        boolean result = tagService.updateTag(tag);
        return Result.success(result);
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> deleteTag(@PathVariable Long id) {
        boolean result = tagService.deleteTag(id);
        return Result.success(result);
    }
}
