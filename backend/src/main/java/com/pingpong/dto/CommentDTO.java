package com.pingpong.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class CommentDTO {
    @NotBlank(message = "评论内容不能为空")
    private String content;
    
    @NotNull(message = "装备不能为空")
    private Long equipmentId;
    
    private Long parentId;
    
    private Integer rating;
    
    private String images;
}
