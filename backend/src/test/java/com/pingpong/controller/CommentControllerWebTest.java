package com.pingpong.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pingpong.common.AdminAccessHelper;
import com.pingpong.config.GlobalExceptionHandler;
import com.pingpong.entity.Comment;
import com.pingpong.service.CommentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CommentControllerWebTest {

    @Mock
    private CommentService commentService;

    @Mock
    private AdminAccessHelper adminAccessHelper;

    @InjectMocks
    private CommentController commentController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(commentController, "commentService", commentService);
        ReflectionTestUtils.setField(commentController, "adminAccessHelper", adminAccessHelper);
        mockMvc = MockMvcBuilders.standaloneSetup(commentController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void adminListShouldRejectNonAdmin() throws Exception {
        doThrow(new RuntimeException("无权执行后台操作")).when(adminAccessHelper).requireAdmin(any());

        mockMvc.perform(get("/comment/admin/list").requestAttr("userId", 2L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500))
            .andExpect(jsonPath("$.msg").value("无权执行后台操作"));
    }

    @Test
    void adminListShouldReturnPagedComments() throws Exception {
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setContent("等待审核的评论");

        Page<Comment> page = new Page<>(1, 10);
        page.setRecords(Collections.singletonList(comment));
        page.setTotal(1);

        when(adminAccessHelper.requireAdmin(any())).thenReturn(1L);
        when(commentService.getAdminComments(1, 10, null, null, null, null, null)).thenReturn(page);

        mockMvc.perform(get("/comment/admin/list").requestAttr("userId", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.records[0].id").value(1));
    }

    @Test
    void reviewCommentShouldSucceedForAdmin() throws Exception {
        when(adminAccessHelper.requireAdmin(any())).thenReturn(1L);
        when(commentService.reviewComment(1L, 8L, 1, "内容真实")).thenReturn(true);

        mockMvc.perform(put("/comment/admin/review")
                .requestAttr("userId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"commentId\":8,\"reviewStatus\":1,\"reviewRemark\":\"内容真实\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").value(true));
    }
}
