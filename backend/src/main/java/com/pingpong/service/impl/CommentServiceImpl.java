package com.pingpong.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pingpong.dto.CommentDTO;
import com.pingpong.entity.Comment;
import com.pingpong.entity.Equipment;
import com.pingpong.entity.User;
import com.pingpong.entity.CommentLike;
import com.pingpong.mapper.CommentLikeMapper;
import com.pingpong.mapper.CommentMapper;
import com.pingpong.mapper.EquipmentMapper;
import com.pingpong.mapper.UserMapper;
import com.pingpong.realtime.RealtimeEventPublisher;
import com.pingpong.realtime.RealtimeEventType;
import com.pingpong.service.CommentService;
import com.pingpong.service.LeaderboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl implements CommentService {

    private static final int MAX_COMMENT_IMAGE_COUNT = 4;
    private static final int REVIEW_PENDING = 0;
    private static final int REVIEW_APPROVED = 1;
    private static final int REVIEW_REJECTED = 2;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private EquipmentMapper equipmentMapper;

    @Autowired
    private CommentLikeMapper commentLikeMapper;

    @Autowired
    private LeaderboardService leaderboardService;

    @Autowired
    private RealtimeEventPublisher realtimeEventPublisher;

    @Override
    @Transactional
    public boolean addComment(Long userId, CommentDTO commentDTO) {
        User currentUser = userMapper.selectById(userId);
        boolean adminDirectPublish = currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getRole());

        Comment comment = new Comment();
        comment.setUserId(userId);
        comment.setEquipmentId(commentDTO.getEquipmentId());
        comment.setParentId(commentDTO.getParentId() == null ? 0L : commentDTO.getParentId());
        comment.setContent(commentDTO.getContent());
        comment.setImages(normalizeCommentImages(commentDTO.getImages()));
        comment.setRating(commentDTO.getRating() != null ? commentDTO.getRating() : 5);
        comment.setLikeCount(0);
        comment.setReplyCount(0);
        comment.setStatus(1);
        comment.setReviewStatus(adminDirectPublish ? REVIEW_APPROVED : REVIEW_PENDING);
        if (adminDirectPublish) {
            comment.setReviewedBy(userId);
            comment.setReviewedAt(LocalDateTime.now());
            comment.setReviewRemark("管理员直发免审");
        }

        boolean result = commentMapper.insert(comment) > 0;

        if (result && commentDTO.getParentId() != null) {
            Comment parentComment = commentMapper.selectById(commentDTO.getParentId());
            if (parentComment != null) {
                parentComment.setReplyCount(defaultInteger(parentComment.getReplyCount()) + 1);
                commentMapper.updateById(parentComment);
            }
        }

        // Bug-10 fix: refresh equipment rating when top-level comment is added
        if (result && isTopLevelComment(comment)) {
            refreshEquipmentRating(comment.getEquipmentId());
        }

        if (result) {
            leaderboardService.recalculateAllBoards(true);
            realtimeEventPublisher.publish(RealtimeEventType.COMMENT_CHANGED, buildCommentPayload(
                adminDirectPublish ? "publish" : "submit",
                comment
            ));
        }

        return result;
    }

    @Override
    public Page<Comment> getEquipmentComments(Long equipmentId, Integer pageNum, Integer pageSize, Long currentUserId) {
        Page<Comment> page = new Page<>(pageNum, pageSize);
        Page<Comment> result = commentMapper.selectPage(page,
            new LambdaQueryWrapper<Comment>()
                .eq(Comment::getEquipmentId, equipmentId)
                .eq(Comment::getParentId, 0L)
                .eq(Comment::getStatus, 1)
                .eq(Comment::getReviewStatus, REVIEW_APPROVED)
                .orderByDesc(Comment::getCreateTime)
        );
        fillCommentUsers(result.getRecords());
        fillCommentLikeState(result.getRecords(), currentUserId);
        return result;
    }

    @Override
    public List<Comment> getReplyComments(Long commentId, Long currentUserId) {
        List<Comment> replies = commentMapper.selectList(
            new LambdaQueryWrapper<Comment>()
                .eq(Comment::getParentId, commentId)
                .eq(Comment::getStatus, 1)
                .eq(Comment::getReviewStatus, REVIEW_APPROVED)
                .orderByAsc(Comment::getCreateTime)
        );
        fillCommentUsers(replies);
        fillCommentLikeState(replies, currentUserId);
        return replies;
    }

    @Override
    @Transactional
    public boolean likeComment(Long userId, Long commentId) {
        if (userId == null) {
            throw new RuntimeException("请先登录");
        }
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null || comment.getStatus() == null || comment.getStatus() != 1) {
            return false;
        }
        if (comment.getReviewStatus() == null || comment.getReviewStatus() != REVIEW_APPROVED) {
            return false;
        }

        // Bug-4 fix: Toggle like with de-duplication
        CommentLike existingLike = commentLikeMapper.selectAnyByUserIdAndCommentId(userId, commentId);

        if (existingLike != null && defaultInteger(existingLike.getDeleted()) == 0) {
            // Already liked, unlike it.
            commentLikeMapper.updateDeletedState(existingLike.getId(), 1);
            comment.setLikeCount(Math.max(0, defaultInteger(comment.getLikeCount()) - 1));
        } else if (existingLike != null) {
            // Restore a logically deleted like record to avoid unique-key collisions.
            commentLikeMapper.updateDeletedState(existingLike.getId(), 0);
            comment.setLikeCount(defaultInteger(comment.getLikeCount()) + 1);
        } else {
            // Not liked yet, add like.
            CommentLike newLike = new CommentLike();
            newLike.setUserId(userId);
            newLike.setCommentId(commentId);
            newLike.setCreateTime(LocalDateTime.now());
            try {
                commentLikeMapper.insert(newLike);
            } catch (DuplicateKeyException ex) {
                CommentLike concurrentLike = commentLikeMapper.selectAnyByUserIdAndCommentId(userId, commentId);
                if (concurrentLike == null) {
                    throw ex;
                }
                if (defaultInteger(concurrentLike.getDeleted()) != 0) {
                    commentLikeMapper.updateDeletedState(concurrentLike.getId(), 0);
                }
            }
            comment.setLikeCount(defaultInteger(comment.getLikeCount()) + 1);
        }

        return commentMapper.updateById(comment) > 0;
    }

    @Override
    @Transactional
    public boolean deleteComment(Long commentId, Long requestUserId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            return false;
        }

        // Bug-2 fix: only comment author or admin can delete
        if (!comment.getUserId().equals(requestUserId)) {
            User requester = userMapper.selectById(requestUserId);
            if (requester == null || !"ADMIN".equalsIgnoreCase(requester.getRole())) {
                throw new RuntimeException("无权删除他人的评论");
            }
        }

        comment.setStatus(0);
        boolean updated = commentMapper.updateById(comment) > 0;
        if (updated && isTopLevelComment(comment)) {
            refreshEquipmentRating(comment.getEquipmentId());
        }
        if (updated) {
            leaderboardService.recalculateAllBoards(true);
            realtimeEventPublisher.publish(RealtimeEventType.COMMENT_CHANGED, buildCommentPayload("delete", comment));
        }
        return updated;
    }

    @Override
    public Page<Comment> getAdminComments(
            Integer pageNum,
            Integer pageSize,
            String keyword,
            Integer status,
            Integer reviewStatus,
            Long userId,
            Long equipmentId) {
        Page<Comment> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<Comment>()
            .like(StringUtils.hasText(keyword), Comment::getContent, keyword)
            .eq(status != null, Comment::getStatus, status)
            .eq(reviewStatus != null, Comment::getReviewStatus, reviewStatus)
            .eq(userId != null, Comment::getUserId, userId)
            .eq(equipmentId != null, Comment::getEquipmentId, equipmentId)
            .orderByDesc(Comment::getCreateTime);

        Page<Comment> result = commentMapper.selectPage(page, queryWrapper);
        fillAdminCommentMeta(result.getRecords());
        return result;
    }

    @Override
    public Comment getAdminCommentDetail(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }

        fillAdminCommentMeta(Collections.singletonList(comment));
        List<Comment> replies = commentMapper.selectList(
            new LambdaQueryWrapper<Comment>()
                .eq(Comment::getParentId, commentId)
                .orderByAsc(Comment::getCreateTime)
        );
        fillAdminCommentMeta(replies);
        comment.setReplies(replies);
        return comment;
    }

    @Override
    @Transactional
    public boolean reviewComment(Long reviewerId, Long commentId, Integer reviewStatus, String reviewRemark) {
        if (reviewStatus == null || (reviewStatus != REVIEW_APPROVED && reviewStatus != REVIEW_REJECTED)) {
            throw new IllegalArgumentException("审核状态不合法");
        }

        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }

        comment.setReviewStatus(reviewStatus);
        comment.setReviewedBy(reviewerId);
        comment.setReviewedAt(LocalDateTime.now());
        comment.setReviewRemark(StringUtils.hasText(reviewRemark) ? reviewRemark.trim() : null);
        boolean updated = commentMapper.updateById(comment) > 0;

        if (updated && isTopLevelComment(comment)) {
            refreshEquipmentRating(comment.getEquipmentId());
        }
        if (updated) {
            leaderboardService.recalculateAllBoards(true);
            realtimeEventPublisher.publish(RealtimeEventType.COMMENT_CHANGED, buildCommentPayload("review", comment));
        }
        return updated;
    }

    @Override
    @Transactional
    public boolean batchDeleteComments(List<Long> commentIds) {
        List<Long> validIds = commentIds == null ? Collections.emptyList() : commentIds.stream()
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
        if (validIds.isEmpty()) {
            throw new IllegalArgumentException("请选择要删除的评论");
        }

        List<Comment> comments = commentMapper.selectBatchIds(validIds);
        if (comments.isEmpty()) {
            return false;
        }

        Set<Long> equipmentIds = comments.stream()
            .filter(this::isTopLevelComment)
            .map(Comment::getEquipmentId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        for (Comment comment : comments) {
            comment.setStatus(0);
            commentMapper.updateById(comment);
        }

        for (Long equipmentId : equipmentIds) {
            refreshEquipmentRating(equipmentId);
        }
        leaderboardService.recalculateAllBoards(true);
        realtimeEventPublisher.publish(RealtimeEventType.COMMENT_CHANGED, buildBatchCommentPayload(validIds, equipmentIds));
        return true;
    }

    @Override
    public Map<String, Long> getAdminCommentStatistics() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", commentMapper.selectCount(new LambdaQueryWrapper<Comment>().eq(Comment::getStatus, 1)));
        stats.put("pending", commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
            .eq(Comment::getStatus, 1)
            .eq(Comment::getReviewStatus, REVIEW_PENDING)));
        stats.put("approved", commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
            .eq(Comment::getStatus, 1)
            .eq(Comment::getReviewStatus, REVIEW_APPROVED)));
        stats.put("rejected", commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
            .eq(Comment::getStatus, 1)
            .eq(Comment::getReviewStatus, REVIEW_REJECTED)));
        return stats;
    }

    private void fillAdminCommentMeta(List<Comment> comments) {
        if (comments == null || comments.isEmpty()) {
            return;
        }

        fillCommentUsers(comments);

        Set<Long> equipmentIds = comments.stream()
            .map(Comment::getEquipmentId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Set<Long> reviewerIds = comments.stream()
            .map(Comment::getReviewedBy)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        Map<Long, String> equipmentNameMap = equipmentIds.isEmpty()
            ? Collections.emptyMap()
            : equipmentMapper.selectBatchIds(equipmentIds).stream()
                .collect(Collectors.toMap(Equipment::getId, Equipment::getName, (left, right) -> left));

        Map<Long, String> reviewerNameMap = reviewerIds.isEmpty()
            ? Collections.emptyMap()
            : userMapper.selectBatchIds(reviewerIds).stream()
                .collect(Collectors.toMap(User::getId, user -> {
                    if (StringUtils.hasText(user.getNickname())) {
                        return user.getNickname();
                    }
                    return user.getUsername();
                }, (left, right) -> left));

        for (Comment comment : comments) {
            comment.setEquipmentName(equipmentNameMap.get(comment.getEquipmentId()));
            comment.setReviewerName(reviewerNameMap.get(comment.getReviewedBy()));
        }
    }

    private void fillCommentUsers(List<Comment> comments) {
        if (comments == null || comments.isEmpty()) {
            return;
        }

        Set<Long> userIds = comments.stream()
            .map(Comment::getUserId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return;
        }

        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
            .collect(Collectors.toMap(User::getId, user -> {
                user.setPassword(null);
                return user;
            }));

        for (Comment comment : comments) {
            comment.setUser(userMap.get(comment.getUserId()));
        }
    }

    private void fillCommentLikeState(List<Comment> comments, Long currentUserId) {
        if (comments == null || comments.isEmpty()) {
            return;
        }

        for (Comment comment : comments) {
            comment.setLikedByCurrentUser(Boolean.FALSE);
        }

        if (currentUserId == null) {
            return;
        }

        Set<Long> commentIds = comments.stream()
            .map(Comment::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (commentIds.isEmpty()) {
            return;
        }

        Set<Long> likedCommentIds = commentLikeMapper.selectList(
            new LambdaQueryWrapper<CommentLike>()
                .eq(CommentLike::getUserId, currentUserId)
                .in(CommentLike::getCommentId, commentIds)
        ).stream()
            .map(CommentLike::getCommentId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        for (Comment comment : comments) {
            comment.setLikedByCurrentUser(likedCommentIds.contains(comment.getId()));
        }
    }

    private void refreshEquipmentRating(Long equipmentId) {
        Equipment equipment = equipmentMapper.selectById(equipmentId);
        if (equipment == null) {
            return;
        }

        List<Comment> comments = commentMapper.selectList(
            new LambdaQueryWrapper<Comment>()
                .eq(Comment::getEquipmentId, equipmentId)
                .eq(Comment::getParentId, 0L)
                .eq(Comment::getStatus, 1)
                .eq(Comment::getReviewStatus, REVIEW_APPROVED)
        );

        List<Comment> ratedComments = comments.stream()
            .filter(comment -> comment.getRating() != null && comment.getRating() > 0)
            .collect(Collectors.toList());

        if (ratedComments.isEmpty()) {
            equipment.setRating(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            equipment.setRatingCount(0);
        } else {
            double totalScore = ratedComments.stream().mapToInt(Comment::getRating).sum();
            equipment.setRating(
                BigDecimal.valueOf(totalScore / ratedComments.size()).setScale(2, RoundingMode.HALF_UP)
            );
            equipment.setRatingCount(ratedComments.size());
        }

        equipmentMapper.updateById(equipment);
    }

    private boolean isTopLevelComment(Comment comment) {
        return comment.getParentId() == null || comment.getParentId() == 0L;
    }

    private int defaultInteger(Integer value) {
        return value == null ? 0 : value;
    }

    private String normalizeCommentImages(String rawImages) {
        List<String> imageUrls = parseImageUrls(rawImages).stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .distinct()
            .collect(Collectors.toList());

        if (imageUrls.isEmpty()) {
            return null;
        }

        if (imageUrls.size() > MAX_COMMENT_IMAGE_COUNT) {
            throw new IllegalArgumentException("晒单图片最多上传 4 张");
        }

        return JSONUtil.toJsonStr(imageUrls);
    }

    private List<String> parseImageUrls(String rawImages) {
        if (!StringUtils.hasText(rawImages)) {
            return new ArrayList<>();
        }

        String trimmed = rawImages.trim();
        if (JSONUtil.isTypeJSONArray(trimmed)) {
            JSONArray array = JSONUtil.parseArray(trimmed);
            List<String> result = new ArrayList<>();
            for (Object item : array) {
                if (item != null) {
                    result.add(String.valueOf(item));
                }
            }
            return result;
        }

        return Arrays.stream(trimmed.split(","))
            .map(String::trim)
            .filter(StringUtils::hasText)
            .collect(Collectors.toList());
    }

    private Map<String, Object> buildCommentPayload(String action, Comment comment) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", action);
        payload.put("commentId", comment.getId());
        payload.put("equipmentId", comment.getEquipmentId());
        payload.put("parentId", comment.getParentId());
        payload.put("reviewStatus", comment.getReviewStatus());
        payload.put("scope", "comment");
        return payload;
    }

    private Map<String, Object> buildBatchCommentPayload(List<Long> commentIds, Set<Long> equipmentIds) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "batch_delete");
        payload.put("commentIds", commentIds);
        payload.put("equipmentIds", equipmentIds);
        payload.put("scope", "comment");
        return payload;
    }
}
