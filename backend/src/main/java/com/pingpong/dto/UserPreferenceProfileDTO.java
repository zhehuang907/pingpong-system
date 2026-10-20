package com.pingpong.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class UserPreferenceProfileDTO {
    private String level;
    private String playStyle;
    private String gripStyle;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private String weeklyFrequency;
    private String primaryGoal;
    private String priorityFocus;
    private List<String> preferredCategories;
    private List<String> preferredBrands;
    private List<String> preferenceTags;
    private Map<String, Object> answerSnapshot;
    private String updatedSource;
}
