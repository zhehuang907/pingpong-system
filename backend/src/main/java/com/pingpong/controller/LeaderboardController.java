package com.pingpong.controller;

import com.pingpong.common.Result;
import com.pingpong.dto.EquipmentLeaderboardPositionDTO;
import com.pingpong.dto.LeaderboardBoardDTO;
import com.pingpong.dto.LeaderboardShareRequestDTO;
import com.pingpong.dto.LeaderboardShareResponseDTO;
import com.pingpong.entity.LeaderboardSubscription;
import com.pingpong.service.LeaderboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/leaderboard")
public class LeaderboardController {

    @Autowired
    private LeaderboardService leaderboardService;

    @GetMapping("/home")
    public Result<List<LeaderboardBoardDTO>> getHomeLeaderboards(
        @RequestParam(required = false, defaultValue = "10") Integer limit
    ) {
        return Result.success(leaderboardService.getHomeLeaderboards(limit));
    }

    @GetMapping("/detail/{boardType}")
    public Result<LeaderboardBoardDTO> getLeaderboardDetail(
        @PathVariable String boardType,
        @RequestParam(required = false, defaultValue = "score") String sortBy,
        @RequestParam(required = false, defaultValue = "10") Integer limit
    ) {
        return Result.success(leaderboardService.getLeaderboardDetail(boardType, sortBy, limit));
    }

    @GetMapping("/equipment/{equipmentId}/positions")
    public Result<List<EquipmentLeaderboardPositionDTO>> getEquipmentRankings(@PathVariable Long equipmentId) {
        return Result.success(leaderboardService.getEquipmentRankings(equipmentId));
    }

    @GetMapping("/subscriptions")
    public Result<List<LeaderboardSubscription>> getUserSubscriptions(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        return Result.success(leaderboardService.getUserSubscriptions(userId));
    }

    @PostMapping("/subscriptions/{boardType}")
    public Result<Boolean> subscribe(@PathVariable String boardType, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        return Result.success(leaderboardService.subscribe(userId, boardType));
    }

    @DeleteMapping("/subscriptions/{boardType}")
    public Result<Boolean> unsubscribe(@PathVariable String boardType, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        return Result.success(leaderboardService.unsubscribe(userId, boardType));
    }

    @PostMapping("/share")
    public Result<LeaderboardShareResponseDTO> recordShare(
        @Validated @RequestBody LeaderboardShareRequestDTO requestDTO,
        HttpServletRequest request
    ) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(leaderboardService.recordShare(userId, requestDTO));
    }

    @GetMapping("/share/stats")
    public Result<Map<String, Object>> getShareStatistics(@RequestParam(required = false) String boardType) {
        return Result.success(leaderboardService.getShareStatistics(boardType));
    }
}
