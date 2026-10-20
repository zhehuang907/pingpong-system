package com.pingpong.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pingpong.entity.Tag;

import java.util.List;

public interface TagService extends IService<Tag> {
    
    List<Tag> getTagList(String category);
    
    boolean addTag(Tag tag);
    
    boolean updateTag(Tag tag);
    
    boolean deleteTag(Long id);
}
