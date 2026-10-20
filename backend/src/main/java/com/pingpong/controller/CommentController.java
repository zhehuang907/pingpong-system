package com.pingpong.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pingpong.common.AdminAccessHelper;
import com.pingpong.common.Result;
import com.pingpong.dto.CommentDTO;
import com.pingpong.entity.Comment;
import com.pingpong.entity.User;
import com.pingpong.mapper.UserMapper;
import com.pingpong.service.CommentService;
import com.pingpong.util.JwtUtil;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/comment")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private AdminAccessHelper adminAccessHelper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping
    public Result<Boolean> addComment(@Valid @RequestBody CommentDTO commentDTO, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "请先登录");
        }

        User currentUser = userMapper.selectById(userId);
        boolean isAdmin = currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getRole());
        boolean result = commentService.addComment(userId, commentDTO);

        return Result.success(isAdmin ? "评论已发布" : "评论已提交，审核后展示", result);
    }

    @GetMapping("/equipment/{equipmentId}")
    public Result<Page<Comment>> getEquipmentComments(
            @PathVariable Long equipmentId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            HttpServletRequest request) {
        Page<Comment> page = commentService.getEquipmentComments(
            equipmentId,
            pageNum,
            pageSize,
            resolveOptionalUserId(request)
        );
        return Result.success(page);
    }

    @GetMapping("/reply/{commentId}")
    public Result<List<Comment>> getReplyComments(@PathVariable Long commentId, HttpServletRequest request) {
        List<Comment> list = commentService.getReplyComments(commentId, resolveOptionalUserId(request));
        return Result.success(list);
    }

    @PostMapping("/like/{commentId}")
    public Result<Boolean> likeComment(@PathVariable Long commentId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        boolean result = commentService.likeComment(userId, commentId);
        return Result.success(result);
    }

    @DeleteMapping("/{commentId}")
    public Result<Boolean> deleteComment(@PathVariable Long commentId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        boolean result = commentService.deleteComment(commentId, userId);
        return Result.success(result);
    }

    @GetMapping("/admin/list")
    public Result<Page<Comment>> getAdminComments(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer reviewStatus,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long equipmentId,
            HttpServletRequest request) {
        adminAccessHelper.requireAdmin(request);
        Page<Comment> page = commentService.getAdminComments(
            pageNum,
            pageSize,
            keyword,
            status,
            reviewStatus,
            userId,
            equipmentId
        );
        return Result.success(page);
    }

    @GetMapping("/admin/{commentId}")
    public Result<Comment> getAdminCommentDetail(@PathVariable Long commentId, HttpServletRequest request) {
        adminAccessHelper.requireAdmin(request);
        return Result.success(commentService.getAdminCommentDetail(commentId));
    }

    @GetMapping("/admin/statistics")
    public Result<Map<String, Long>> getAdminCommentStatistics(HttpServletRequest request) {
        adminAccessHelper.requireAdmin(request);
        return Result.success(commentService.getAdminCommentStatistics());
    }

    @PutMapping("/admin/review")
    public Result<Boolean> reviewComment(@RequestBody CommentReviewRequest reviewRequest, HttpServletRequest request) {
        Long reviewerId = adminAccessHelper.requireAdmin(request);
        if (reviewRequest.getCommentId() == null) {
            return Result.error("评论ID不能为空");
        }
        boolean result = commentService.reviewComment(
            reviewerId,
            reviewRequest.getCommentId(),
            reviewRequest.getReviewStatus(),
            StringUtils.hasText(reviewRequest.getReviewRemark()) ? reviewRequest.getReviewRemark().trim() : null
        );
        return Result.success(result);
    }

    @DeleteMapping("/admin/{commentId}")
    public Result<Boolean> deleteAdminComment(@PathVariable Long commentId, HttpServletRequest request) {
        Long adminId = adminAccessHelper.requireAdmin(request);
        return Result.success(commentService.deleteComment(commentId, adminId));
    }

    @PostMapping("/admin/batch-delete")
    public Result<Boolean> batchDeleteComments(@RequestBody CommentBatchDeleteRequest deleteRequest, HttpServletRequest request) {
        adminAccessHelper.requireAdmin(request);
        return Result.success(commentService.batchDeleteComments(deleteRequest.getIds()));
    }

    @Data
    static class CommentReviewRequest {
        private Long commentId;
        private Integer reviewStatus;
        private String reviewRemark;
    }

    @Data
    static class CommentBatchDeleteRequest {
        private List<Long> ids;
    }

    private Long resolveOptionalUserId(HttpServletRequest request) {
        Object requestUserId = request.getAttribute("userId");
        if (requestUserId instanceof Long) {
            return (Long) requestUserId;
        }

        String authorization = request.getHeader(jwtUtil.getHeader());
        if (!StringUtils.hasText(authorization)) {
            return null;
        }

        String token = authorization.trim();
        String prefix = jwtUtil.getPrefix();
        if (StringUtils.hasText(prefix) && token.regionMatches(true, 0, prefix, 0, prefix.length())) {
            token = token.substring(prefix.length()).trim();
        }

        if (!StringUtils.hasText(token) || !jwtUtil.validateToken(token)) {
            return null;
        }

        try {
            return jwtUtil.getUserIdFromToken(token);
        } catch (Exception ignored) {
            return null;
        }
    }
}
