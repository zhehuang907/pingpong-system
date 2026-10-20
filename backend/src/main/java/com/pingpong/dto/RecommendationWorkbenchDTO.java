package com.pingpong.dto;

import com.pingpong.entity.Equipment;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class RecommendationWorkbenchDTO {
    private String summaryTitle;
    private String summaryDescription;
    private List<String> profileKeywords;
    private List<String> coachSuggestions;
    private List<Map<String, Object>> combinationPlans;
    private List<Map<String, Object>> upgradePlans;
    private List<Map<String, Object>> trendSignals;
    private List<Equipment> newArrivals;
    private List<Map<String, Object>> trialCards;
    private List<Map<String, Object>> playerReferences;
    private List<Map<String, Object>> compareOptions;
    private Map<String, Object> feedbackLoop;
}
