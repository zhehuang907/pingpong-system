package com.pingpong.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pingpong.PingpongRecommendationApplication;
import com.pingpong.dto.CommentDTO;
import com.pingpong.entity.Category;
import com.pingpong.entity.Comment;
import com.pingpong.entity.Equipment;
import com.pingpong.entity.Tag;
import com.pingpong.entity.User;
import com.pingpong.mapper.CommentMapper;
import com.pingpong.mapper.UserMapper;
import com.pingpong.service.CategoryService;
import com.pingpong.service.CommentService;
import com.pingpong.service.EquipmentService;
import com.pingpong.service.TagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SpringBootTest(classes = PingpongRecommendationApplication.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class CoreWritePathIntegrationTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private TagService tagService;

    @Autowired
    private EquipmentService equipmentService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Test
    void coreWritePathsShouldPersistAgainstRuntimeSchema() {
        User firstUser = userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 1)
                .last("LIMIT 1")
        );
        assumeTrue(firstUser != null, "At least one active user is required for write-path smoke testing.");

        long suffix = System.nanoTime();

        Category category = new Category();
        category.setName("结构回归分类-" + suffix);
        category.setParentId(0L);
        category.setLevel(1);
        category.setSort(99);
        category.setStatus(1);

        assertTrue(categoryService.addCategory(category));
        assertNotNull(category.getId());
        assertNotNull(category.getCreateTime());
        assertNotNull(category.getUpdateTime());

        Tag tag = new Tag();
        tag.setName("结构回归标签-" + suffix);
        tag.setCategory("回归测试");
        tag.setColor("#1F7A5A");
        tag.setSort(99);
        tag.setStatus(1);

        assertTrue(tagService.addTag(tag));
        assertNotNull(tag.getId());
        assertNotNull(tag.getCreateTime());
        assertNotNull(tag.getUpdateTime());

        Equipment equipment = new Equipment();
        equipment.setName("结构回归器材-" + suffix);
        equipment.setCategoryId(category.getId());
        equipment.setBrand("SchemaLab");
        equipment.setModel("SMK-" + suffix);
        equipment.setPrice(new BigDecimal("199.00"));
        equipment.setOriginalPrice(new BigDecimal("229.00"));
        equipment.setStock(5);
        equipment.setDescription("用于验证运行时库表结构兼容性的器材数据");
        equipment.setSuitableFor("回归测试");
        equipment.setFeatures("自动化校验");
        equipment.setTags("[]");

        assertTrue(equipmentService.addEquipment(equipment));
        assertNotNull(equipment.getId());
        assertNotNull(equipment.getCreateTime());
        assertNotNull(equipment.getUpdateTime());

        CommentDTO commentDTO = new CommentDTO();
        commentDTO.setEquipmentId(equipment.getId());
        commentDTO.setContent("结构回归评论-" + suffix);
        commentDTO.setRating(5);

        assertTrue(commentService.addComment(firstUser.getId(), commentDTO));

        Comment insertedComment = commentMapper.selectOne(
            new LambdaQueryWrapper<Comment>()
                .eq(Comment::getUserId, firstUser.getId())
                .eq(Comment::getEquipmentId, equipment.getId())
                .eq(Comment::getContent, commentDTO.getContent())
                .last("LIMIT 1")
        );

        assertNotNull(insertedComment);
        assertEquals(Long.valueOf(0L), insertedComment.getParentId());
        assertEquals(Integer.valueOf(5), insertedComment.getRating());
        assertNotNull(insertedComment.getCreateTime());
        assertNotNull(insertedComment.getUpdateTime());
    }
}
