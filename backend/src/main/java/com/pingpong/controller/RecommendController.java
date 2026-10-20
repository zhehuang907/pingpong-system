package com.pingpong.controller;

import com.pingpong.common.Result;
import com.pingpong.dto.RecommendationWorkbenchDTO;
import com.pingpong.dto.UserProfileInsightDTO;
import com.pingpong.entity.Equipment;
import com.pingpong.exception.BusinessException;
import com.pingpong.service.RecommendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/recommend")
public class RecommendController {

    @Autowired
    private RecommendService recommendService;

    @GetMapping("/personal")
    public Result<List<Equipment>> getPersonalRecommend(
            HttpServletRequest request,
            @RequestParam(required = false, defaultValue = "20") Integer count) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        List<Equipment> list = recommendService.getPersonalRecommend(userId, count);
        return Result.success(list);
    }

    @GetMapping("/similar/{equipmentId}")
    public Result<List<Equipment>> getSimilarRecommend(
            @PathVariable Long equipmentId,
            @RequestParam(required = false, defaultValue = "5") Integer count) {
        List<Equipment> list = recommendService.getSimilarRecommend(equipmentId, count);
        return Result.success(list);
    }

    @GetMapping("/hot")
    public Result<List<Equipment>> getHotRecommend(
            @RequestParam(required = false, defaultValue = "10") Integer count) {
        List<Equipment> list = recommendService.getHotRecommend(count);
        return Result.success(list);
    }

    @GetMapping("/analyze")
    public Result<Map<String, Object>> analyzeUserBehavior(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        Map<String, Object> analysis = recommendService.analyzeUserBehavior(userId);
        return Result.success(analysis);
    }

    @GetMapping("/profile-insight")
    public Result<UserProfileInsightDTO> getUserProfileInsight(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        UserProfileInsightDTO insight = recommendService.getUserProfileInsight(userId);
        return Result.success(insight);
    }

    @GetMapping("/workbench")
    public Result<RecommendationWorkbenchDTO> getRecommendationWorkbench(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        RecommendationWorkbenchDTO workbench = recommendService.getRecommendationWorkbench(userId);
        return Result.success(workbench);
    }

    @PostMapping("/click/{equipmentId}")
    public Result<Boolean> recordClick(
            @PathVariable Long equipmentId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId != null) {
            recommendService.recordRecommendClick(userId, equipmentId);
        }
        return Result.success(true);
    }
}
