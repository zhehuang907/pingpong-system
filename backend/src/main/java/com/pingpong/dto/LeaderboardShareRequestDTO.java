package com.pingpong.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class LeaderboardShareRequestDTO {

    @NotBlank(message = "榜单类型不能为空")
    private String boardType;

    private Long equipmentId;

    @NotBlank(message = "分享渠道不能为空")
    private String channel;
}
