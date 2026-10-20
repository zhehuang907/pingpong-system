package com.pingpong.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pingpong.entity.Tag;
import com.pingpong.mapper.TagMapper;
import com.pingpong.service.TagService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

    @Override
    @Cacheable(value = "tag:list", key = "T(java.util.Arrays).toString(#root.args)")
    public List<Tag> getTagList(String category) {
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Tag::getStatus, 1);
        if (category != null && !category.isEmpty()) {
            wrapper.eq(Tag::getCategory, category);
        }
        wrapper.orderByAsc(Tag::getSort);
        return this.list(wrapper);
    }

    @Override
    @Transactional
    public boolean addTag(Tag tag) {
        if (tag.getSort() == null) {
            tag.setSort(0);
        }
        if (tag.getStatus() == null) {
            tag.setStatus(1);
        }
        if (tag.getColor() == null) {
            tag.setColor("#409EFF");
        }
        return this.save(tag);
    }

    @Override
    @Transactional
    public boolean updateTag(Tag tag) {
        return this.updateById(tag);
    }

    @Override
    @Transactional
    public boolean deleteTag(Long id) {
        return this.removeById(id);
    }
}
