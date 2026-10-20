package com.pingpong.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pingpong.dto.CommentDTO;
import com.pingpong.entity.Comment;

import java.util.List;
import java.util.Map;

public interface CommentService {
    
    boolean addComment(Long userId, CommentDTO commentDTO);
    
    Page<Comment> getEquipmentComments(Long equipmentId, Integer pageNum, Integer pageSize, Long currentUserId);

    List<Comment> getReplyComments(Long commentId, Long currentUserId);
    
    boolean likeComment(Long userId, Long commentId);
    
    boolean deleteComment(Long commentId, Long requestUserId);

    Page<Comment> getAdminComments(
        Integer pageNum,
        Integer pageSize,
        String keyword,
        Integer status,
        Integer reviewStatus,
        Long userId,
        Long equipmentId
    );

    Comment getAdminCommentDetail(Long commentId);

    boolean reviewComment(Long reviewerId, Long commentId, Integer reviewStatus, String reviewRemark);

    boolean batchDeleteComments(List<Long> commentIds);

    Map<String, Long> getAdminCommentStatistics();
}
