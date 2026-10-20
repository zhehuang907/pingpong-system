package com.pingpong.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pingpong.dto.CommentDTO;
import com.pingpong.entity.Comment;
import com.pingpong.entity.CommentLike;
import com.pingpong.entity.Equipment;
import com.pingpong.entity.User;
import com.pingpong.mapper.CommentLikeMapper;
import com.pingpong.mapper.CommentMapper;
import com.pingpong.mapper.EquipmentMapper;
import com.pingpong.mapper.UserMapper;
import com.pingpong.realtime.RealtimeEventPublisher;
import com.pingpong.service.LeaderboardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private EquipmentMapper equipmentMapper;

    @Mock
    private CommentLikeMapper commentLikeMapper;

    @Mock
    private LeaderboardService leaderboardService;

    @Mock
    private RealtimeEventPublisher realtimeEventPublisher;

    @InjectMocks
    private CommentServiceImpl commentService;

    @Test
    void addCommentShouldBindUserAndMarkAsPendingReview() {
        CommentDTO dto = new CommentDTO();
        dto.setEquipmentId(10L);
        dto.setContent("Great control");
        dto.setRating(5);
        dto.setImages("[\"/api/upload/comment-1.png\",\"/api/upload/comment-2.png\"]");

        when(commentMapper.insert(any(Comment.class))).thenReturn(1);

        boolean result = commentService.addComment(7L, dto);

        assertTrue(result);

        ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);
        verify(commentMapper).insert(commentCaptor.capture());
        assertEquals(Long.valueOf(7L), commentCaptor.getValue().getUserId());
        assertEquals(Long.valueOf(0L), commentCaptor.getValue().getParentId());
        assertEquals(Integer.valueOf(0), commentCaptor.getValue().getLikeCount());
        assertEquals(Integer.valueOf(0), commentCaptor.getValue().getReplyCount());
        assertEquals(Integer.valueOf(0), commentCaptor.getValue().getReviewStatus());
        assertEquals("[\"/api/upload/comment-1.png\",\"/api/upload/comment-2.png\"]", commentCaptor.getValue().getImages());
    }

    @Test
    void addReplyShouldIncreaseParentReplyCount() {
        CommentDTO dto = new CommentDTO();
        dto.setEquipmentId(10L);
        dto.setParentId(20L);
        dto.setContent("I agree");
        dto.setRating(4);

        Comment parent = new Comment();
        parent.setId(20L);
        parent.setReplyCount(2);

        when(commentMapper.insert(any(Comment.class))).thenReturn(1);
        when(commentMapper.selectById(20L)).thenReturn(parent);

        boolean result = commentService.addComment(7L, dto);

        assertTrue(result);
        ArgumentCaptor<Comment> parentCaptor = ArgumentCaptor.forClass(Comment.class);
        verify(commentMapper).updateById(parentCaptor.capture());
        assertEquals(Integer.valueOf(3), parentCaptor.getValue().getReplyCount());
    }

    @Test
    void getEquipmentCommentsShouldAttachUserInfo() {
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setUserId(7L);

        when(commentMapper.selectPage(any(Page.class), any())).thenAnswer(invocation -> {
            Page<Comment> page = invocation.getArgument(0);
            page.setRecords(Collections.singletonList(comment));
            return page;
        });

        User user = new User();
        user.setId(7L);
        user.setUsername("tester");
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(Collections.singletonList(user));

        Page<Comment> page = commentService.getEquipmentComments(10L, 1, 10, null);

        assertEquals("tester", page.getRecords().get(0).getUser().getUsername());
    }

    @Test
    void addCommentShouldRejectTooManyImages() {
        CommentDTO dto = new CommentDTO();
        dto.setEquipmentId(10L);
        dto.setContent("Great control");
        dto.setRating(5);
        dto.setImages("[\"1.png\",\"2.png\",\"3.png\",\"4.png\",\"5.png\"]");

        assertThrows(IllegalArgumentException.class, () -> commentService.addComment(7L, dto));
    }

    @Test
    void likeCommentShouldRecoverFromConcurrentDuplicateInsert() {
        Comment comment = new Comment();
        comment.setId(18L);
        comment.setLikeCount(0);
        comment.setStatus(1);
        comment.setReviewStatus(1);

        CommentLike concurrentLike = new CommentLike();
        concurrentLike.setId(30L);
        concurrentLike.setUserId(7L);
        concurrentLike.setCommentId(18L);
        concurrentLike.setDeleted(0);

        when(commentMapper.selectById(18L)).thenReturn(comment);
        when(commentLikeMapper.selectAnyByUserIdAndCommentId(7L, 18L)).thenReturn(null, concurrentLike);
        when(commentLikeMapper.insert(any(CommentLike.class))).thenThrow(new DuplicateKeyException("duplicate like row"));
        when(commentMapper.updateById(any(Comment.class))).thenReturn(1);

        boolean result = commentService.likeComment(7L, 18L);

        assertTrue(result);
        ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);
        verify(commentMapper).updateById(commentCaptor.capture());
        assertEquals(Integer.valueOf(1), commentCaptor.getValue().getLikeCount());
    }

    @Test
    void reviewCommentShouldRefreshEquipmentRatingForApprovedTopLevelComment() {
        Comment comment = new Comment();
        comment.setId(18L);
        comment.setEquipmentId(10L);
        comment.setParentId(0L);
        comment.setStatus(1);

        Equipment equipment = new Equipment();
        equipment.setId(10L);
        equipment.setRating(BigDecimal.ZERO);
        equipment.setRatingCount(0);

        when(commentMapper.selectById(18L)).thenReturn(comment);
        when(commentMapper.updateById(any(Comment.class))).thenReturn(1);
        when(equipmentMapper.selectById(10L)).thenReturn(equipment);
        when(commentMapper.selectList(any())).thenReturn(Collections.singletonList(buildRatedApprovedComment(5)));

        boolean result = commentService.reviewComment(1L, 18L, 1, "内容真实");

        assertTrue(result);

        ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);
        verify(commentMapper).updateById(commentCaptor.capture());
        assertEquals(Integer.valueOf(1), commentCaptor.getValue().getReviewStatus());
        assertEquals(Long.valueOf(1L), commentCaptor.getValue().getReviewedBy());

        ArgumentCaptor<Equipment> equipmentCaptor = ArgumentCaptor.forClass(Equipment.class);
        verify(equipmentMapper).updateById(equipmentCaptor.capture());
        assertEquals(new BigDecimal("5.00"), equipmentCaptor.getValue().getRating());
        assertEquals(Integer.valueOf(1), equipmentCaptor.getValue().getRatingCount());
    }

    private Comment buildRatedApprovedComment(int rating) {
        Comment comment = new Comment();
        comment.setRating(rating);
        comment.setParentId(0L);
        comment.setStatus(1);
        comment.setReviewStatus(1);
        return comment;
    }
}
