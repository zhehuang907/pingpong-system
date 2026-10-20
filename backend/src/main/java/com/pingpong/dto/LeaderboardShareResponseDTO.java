package com.pingpong.dto;

import lombok.Data;

@Data
public class LeaderboardShareResponseDTO {
    private String boardType;
    private Long equipmentId;
    private String channel;
    private String title;
    private String shareText;
    private String shareUrl;
    private String posterSvg;
}
