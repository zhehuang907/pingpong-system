package com.pingpong.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserProfileInsightDTO {
    private Integer completionScore;
    private String recommendationMode;
    private String personaTitle;
    private String personaDescription;
    private List<String> coreTags;
    private List<String> improvementActions;
    private List<UserProfileInsightSignalDTO> signals;
    private List<UserProfileActionDTO> actionCards;
}
