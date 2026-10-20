package com.pingpong.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("comment")
public class Comment implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long equipmentId;

    private Long parentId;

    private String content;

    private String images;

    private Integer rating;

    private Integer likeCount;

    private Integer replyCount;

    private Integer status;
    
    private Integer reviewStatus;
    
    private Long reviewedBy;
    
    private LocalDateTime reviewedAt;
    
    private String reviewRemark;

    @TableField(exist = false)
    private User user;

    @TableField(exist = false)
    private List<Comment> replies;

    @TableField(exist = false)
    private Boolean likedByCurrentUser;

    @TableField(exist = false)
    private String equipmentName;

    @TableField(exist = false)
    private String reviewerName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
