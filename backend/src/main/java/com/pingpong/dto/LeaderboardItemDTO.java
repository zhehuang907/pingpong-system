package com.pingpong.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class LeaderboardItemDTO {
    private Long equipmentId;
    private String name;
    private String brand;
    private String model;
    private String mainImage;
    private BigDecimal price;
    private String suitableFor;
    private BigDecimal equipmentRating;
    private Integer rankPosition;
    private BigDecimal totalScore;
    private BigDecimal hotScore;
    private BigDecimal valueScore;
    private Integer favoriteCount;
    private Integer commentCount;
    private BigDecimal avgRating;
    private BigDecimal satisfactionRate;
    private String boardReason;
    private LocalDateTime latestInteractionTime;
}
