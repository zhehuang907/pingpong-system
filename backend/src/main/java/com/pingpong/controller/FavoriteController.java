package com.pingpong.controller;

import com.pingpong.common.Result;
import com.pingpong.entity.Favorite;
import com.pingpong.exception.BusinessException;
import com.pingpong.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.pingpong.service.impl.FavoriteServiceImpl;

@RestController
@RequestMapping("/favorite")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    @PostMapping("/toggle/{equipmentId}")
    public Result<Map<String, Object>> toggleFavorite(
            @PathVariable Long equipmentId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        FavoriteServiceImpl.FavoriteToggleResult toggleResult = favoriteService.toggleFavorite(userId, equipmentId);
        
        Map<String, Object> data = new HashMap<>();
        data.put("success", true);
        data.put("isFavorite", toggleResult.isFavorite());
        data.put("favoriteCount", toggleResult.getFavoriteCount());
        
        return Result.success(data);
    }

    @DeleteMapping("/cancel/{equipmentId}")
    public Result<Boolean> cancelFavorite(
            @PathVariable Long equipmentId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        boolean result = favoriteService.cancelFavorite(userId, equipmentId);
        return Result.success(result);
    }

    @GetMapping("/list")
    public Result<List<Favorite>> getUserFavorites(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        List<Favorite> list = favoriteService.getUserFavorites(userId);
        return Result.success(list);
    }

    @GetMapping("/check/{equipmentId}")
    public Result<Boolean> checkFavorite(
            @PathVariable Long equipmentId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        boolean isFavorite = favoriteService.isFavorite(userId, equipmentId);
        return Result.success(isFavorite);
    }
}
