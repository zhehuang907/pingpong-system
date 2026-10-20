package com.pingpong.dto;

import com.pingpong.entity.Equipment;
import lombok.Data;

import java.util.List;

@Data
public class KickoffKingOverviewDTO {
    private SeasonCard season;
    private List<StageCard> stages;
    private List<StandingCard> standings;
    private List<LeaderboardBoardDTO> featuredBoards;
    private List<Equipment> featuredEquipments;
    private PersonalizedTrack personalizedTrack;

    @Data
    public static class SeasonCard {
        private String seasonCode;
        private String seasonName;
        private String heroTitle;
        private String heroSubtitle;
        private String announcement;
        private String seasonStatus;
        private Integer totalStops;
        private Integer completedStops;
        private String currentStopName;
        private Integer progressPercent;
        private List<String> highlightTags;
    }

    @Data
    public static class StageCard {
        private Long id;
        private String stageName;
        private String cityName;
        private String stageType;
        private String stageStatus;
        private String stageDate;
        private String theme;
        private String focusSkill;
        private String highlightDesc;
    }

    @Data
    public static class StandingCard {
        private Integer rankPosition;
        private String playerName;
        private String associationName;
        private Integer points;
        private Integer stageWins;
        private Integer podiumCount;
        private String serveStyle;
        private String hotForm;
    }

    @Data
    public static class PersonalizedTrack {
        private String title;
        private String description;
        private String recommendedBoardType;
        private String recommendedBoardName;
        private String equipmentKeyword;
        private List<String> tags;
    }
}
