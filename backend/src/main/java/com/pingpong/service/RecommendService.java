package com.pingpong.service;

import com.pingpong.dto.RecommendationWorkbenchDTO;
import com.pingpong.dto.UserProfileInsightDTO;
import com.pingpong.entity.Equipment;

import java.util.List;
import java.util.Map;

public interface RecommendService {
    
    List<Equipment> getPersonalRecommend(Long userId, Integer count);
    
    List<Equipment> getSimilarRecommend(Long equipmentId, Integer count);
    
    List<Equipment> getHotRecommend(Integer count);
    
    Map<String, Object> analyzeUserBehavior(Long userId);

    UserProfileInsightDTO getUserProfileInsight(Long userId);

    RecommendationWorkbenchDTO getRecommendationWorkbench(Long userId);
    
    void recordRecommend(Long userId, Long equipmentId, String recommendType, Double score, String reason);
    
    void recordRecommendClick(Long userId, Long equipmentId);
}
