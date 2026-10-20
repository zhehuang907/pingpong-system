package com.pingpong.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class EquipmentLeaderboardPositionDTO {
    private String boardType;
    private String boardName;
    private Integer rankPosition;
    private BigDecimal totalScore;
    private String boardReason;
}
