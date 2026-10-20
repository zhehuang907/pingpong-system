package com.pingpong.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class LeaderboardBoardDTO {
    private String boardType;
    private String boardName;
    private String subtitle;
    private String sortBy;
    private LocalDateTime refreshedAt;
    private List<LeaderboardItemDTO> items;
}
