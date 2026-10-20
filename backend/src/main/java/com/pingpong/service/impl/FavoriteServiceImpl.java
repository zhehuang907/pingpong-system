package com.pingpong.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pingpong.entity.Equipment;
import com.pingpong.entity.Favorite;
import com.pingpong.mapper.EquipmentMapper;
import com.pingpong.mapper.FavoriteMapper;
import com.pingpong.exception.BusinessException;
import com.pingpong.service.EquipmentService;
import com.pingpong.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FavoriteServiceImpl implements FavoriteService {

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private EquipmentMapper equipmentMapper;

    @Autowired
    private EquipmentService equipmentService;

    @Override
    @Transactional
    public FavoriteToggleResult toggleFavorite(Long userId, Long equipmentId) {
        Equipment equipment = equipmentMapper.selectById(equipmentId);
        if (equipment == null) {
            throw new BusinessException("该器材不存在或已下架");
        }

        Favorite existing = favoriteMapper.selectOne(
            new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getEquipmentId, equipmentId)
        );

        boolean isFavorite;
        if (existing != null) {
            favoriteMapper.deleteById(existing.getId());
            equipmentService.updateFavoriteCount(equipmentId, -1);
            isFavorite = false;
        } else {
            Favorite favorite = new Favorite();
            favorite.setUserId(userId);
            favorite.setEquipmentId(equipmentId);
            LocalDateTime now = LocalDateTime.now();
            favorite.setCreateTime(now);
            favorite.setUpdateTime(now);
            favoriteMapper.insert(favorite);
            equipmentService.updateFavoriteCount(equipmentId, 1);
            isFavorite = true;
        }

        int count = equipment.getFavoriteCount() == null ? 0 : equipment.getFavoriteCount();
        count = Math.max(0, count + (isFavorite ? 1 : -1));
        
        return new FavoriteToggleResult(isFavorite, count);
    }

    public static class FavoriteToggleResult {
        private final boolean isFavorite;
        private final int favoriteCount;

        public FavoriteToggleResult(boolean isFavorite, int favoriteCount) {
            this.isFavorite = isFavorite;
            this.favoriteCount = favoriteCount;
        }

        public boolean isFavorite() { return isFavorite; }
        public int getFavoriteCount() { return favoriteCount; }
    }

    @Override
    @Transactional
    public boolean cancelFavorite(Long userId, Long equipmentId) {
        Equipment equipment = equipmentMapper.selectById(equipmentId);
        if (equipment == null) {
            throw new BusinessException("该器材不存在或已下架");
        }

        boolean removed = favoriteMapper.delete(
            new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getEquipmentId, equipmentId)
        ) > 0;
        if (removed) {
            equipmentService.updateFavoriteCount(equipmentId, -1);
        }
        return removed;
    }

    @Override
    public List<Favorite> getUserFavorites(Long userId) {
        List<Favorite> favorites = favoriteMapper.selectList(
            new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .orderByDesc(Favorite::getCreateTime)
        );

        if (favorites.isEmpty()) {
            return favorites;
        }

        Set<Long> equipmentIds = favorites.stream()
            .map(Favorite::getEquipmentId)
            .collect(Collectors.toSet());
        Map<Long, Equipment> equipmentMap = equipmentMapper.selectBatchIds(equipmentIds).stream()
            .collect(Collectors.toMap(Equipment::getId, equipment -> equipment));

        for (Favorite favorite : favorites) {
            favorite.setEquipment(equipmentMap.get(favorite.getEquipmentId()));
        }

        return favorites;
    }

    @Override
    public boolean isFavorite(Long userId, Long equipmentId) {
        return favoriteMapper.selectCount(
            new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getEquipmentId, equipmentId)
        ) > 0;
    }
}
