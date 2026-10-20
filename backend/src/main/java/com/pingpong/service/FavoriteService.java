package com.pingpong.service;

import com.pingpong.entity.Favorite;

import java.util.List;


import com.pingpong.service.impl.FavoriteServiceImpl;

public interface FavoriteService {
    
    FavoriteServiceImpl.FavoriteToggleResult toggleFavorite(Long userId, Long equipmentId);
    
    boolean cancelFavorite(Long userId, Long equipmentId);
    
    List<Favorite> getUserFavorites(Long userId);
    
    boolean isFavorite(Long userId, Long equipmentId);
}
