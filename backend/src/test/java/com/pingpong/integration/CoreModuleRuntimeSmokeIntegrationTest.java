package com.pingpong.integration;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pingpong.PingpongRecommendationApplication;
import com.pingpong.entity.Category;
import com.pingpong.entity.Comment;
import com.pingpong.entity.Equipment;
import com.pingpong.entity.Tag;
import com.pingpong.service.CategoryService;
import com.pingpong.service.CommentService;
import com.pingpong.service.EquipmentService;
import com.pingpong.service.RecommendService;
import com.pingpong.service.TagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = PingpongRecommendationApplication.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CoreModuleRuntimeSmokeIntegrationTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private TagService tagService;

    @Autowired
    private EquipmentService equipmentService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private RecommendService recommendService;

    @Test
    void coreReadPathsShouldRunAgainstRuntimeSchema() {
        List<Category> categories = assertDoesNotThrow(() -> categoryService.getCategoryList(null));
        List<Category> categoryTree = assertDoesNotThrow(categoryService::getCategoryTree);
        List<Tag> tags = assertDoesNotThrow(() -> tagService.getTagList(null));
        Page<Equipment> equipmentPage = assertDoesNotThrow(
            () -> equipmentService.getEquipmentList(1, 5, null, null, null, null, "rating")
        );
        Page<Comment> equipmentComments = assertDoesNotThrow(
            () -> commentService.getEquipmentComments(-1L, 1, 5, null)
        );
        List<Comment> replyComments = assertDoesNotThrow(() -> commentService.getReplyComments(-1L, null));
        List<Equipment> personalRecommend = assertDoesNotThrow(
            () -> recommendService.getPersonalRecommend(-1L, 3)
        );
        Map<String, Object> behaviorAnalysis = assertDoesNotThrow(
            () -> recommendService.analyzeUserBehavior(-1L)
        );
        List<Equipment> hotRecommend = assertDoesNotThrow(() -> recommendService.getHotRecommend(3));

        assertNotNull(categories);
        assertNotNull(categoryTree);
        assertNotNull(tags);
        assertNotNull(equipmentPage);
        assertNotNull(equipmentComments);
        assertNotNull(replyComments);
        assertNotNull(personalRecommend);
        assertNotNull(behaviorAnalysis);
        assertNotNull(hotRecommend);
    }
}
