package com.pingpong.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pingpong.entity.Category;
import com.pingpong.entity.Equipment;
import com.pingpong.mapper.CategoryMapper;
import com.pingpong.mapper.EquipmentMapper;
import com.pingpong.realtime.RealtimeEventPublisher;
import com.pingpong.realtime.RealtimeEventType;
import com.pingpong.service.EquipmentService;
import com.pingpong.service.LeaderboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class EquipmentServiceImpl extends ServiceImpl<EquipmentMapper, Equipment> implements EquipmentService {

    @Autowired
    private LeaderboardService leaderboardService;

    @Autowired
    private RealtimeEventPublisher realtimeEventPublisher;

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    @Cacheable(value = "equipment:list", key = "T(java.util.Arrays).toString(#root.args)")
    public Page<Equipment> getEquipmentList(Integer pageNum, Integer pageSize, Long categoryId, String keyword, Double minPrice, Double maxPrice, String sortBy) {
        Page<Equipment> page = new Page<>(pageNum, pageSize);
        
        LambdaQueryWrapper<Equipment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Equipment::getStatus, 1);
        
        if (categoryId != null) {
            wrapper.eq(Equipment::getCategoryId, categoryId);
        }
        
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w
                .like(Equipment::getName, keyword)
                .or()
                .like(Equipment::getBrand, keyword)
                .or()
                .like(Equipment::getModel, keyword)
                .or()
                .like(Equipment::getDescription, keyword)
                .or()
                .like(Equipment::getFeatures, keyword)
                .or()
                .like(Equipment::getSuitableFor, keyword)
            );
        }

        if (minPrice != null) {
            wrapper.ge(Equipment::getPrice, minPrice);
        }

        if (maxPrice != null) {
            wrapper.le(Equipment::getPrice, maxPrice);
        }
        
        if ("price_asc".equals(sortBy)) {
            wrapper.orderByAsc(Equipment::getPrice);
        } else if ("price_desc".equals(sortBy)) {
            wrapper.orderByDesc(Equipment::getPrice);
        } else if ("sales".equals(sortBy)) {
            wrapper.orderByDesc(Equipment::getSalesCount);
        } else if ("rating".equals(sortBy)) {
            wrapper.orderByDesc(Equipment::getRating);
        } else {
            wrapper.orderByDesc(Equipment::getCreateTime);
        }
        
        return this.page(page, wrapper);
    }

    @Override
    @Cacheable(value = "equipment:detail", key = "#id")
    public Equipment getEquipmentDetail(Long id) {
        return getEquipmentDetail(id, true);
    }

    @Override
    @Cacheable(value = "equipment:detail", key = "#id + ':' + #trackView")
    public Equipment getEquipmentDetail(Long id, boolean trackView) {
        Equipment equipment = this.getById(id);
        if (equipment == null || equipment.getStatus() == null || equipment.getStatus() != 1) {
            return null;
        }
        if (trackView) {
            updateViewCount(id);
            equipment.setViewCount(defaultInteger(equipment.getViewCount()) + 1);
        }
        return equipment;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"equipment:list", "equipment:detail"}, allEntries = true)
    public boolean addEquipment(Equipment equipment) {
        normalizeImageFields(equipment);
        equipment.setStatus(1);
        equipment.setViewCount(0);
        equipment.setFavoriteCount(0);
        equipment.setSalesCount(0);
        equipment.setRating(new BigDecimal("0.00"));
        equipment.setRatingCount(0);
        boolean saved = this.save(equipment);
        if (saved) {
            leaderboardService.recalculateAllBoards(true);
            realtimeEventPublisher.publish(RealtimeEventType.EQUIPMENT_CHANGED, buildEquipmentPayload("add", equipment.getId()));
        }
        return saved;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"equipment:list", "equipment:detail"}, allEntries = true)
    public boolean updateEquipment(Equipment equipment) {
        normalizeImageFields(equipment);
        boolean updated = this.updateById(equipment);
        if (updated) {
            leaderboardService.recalculateAllBoards(true);
            realtimeEventPublisher.publish(RealtimeEventType.EQUIPMENT_CHANGED, buildEquipmentPayload("update", equipment.getId()));
        }
        return updated;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"equipment:list", "equipment:detail"}, allEntries = true)
    public boolean deleteEquipment(Long id) {
        boolean removed = this.removeById(id);
        if (removed) {
            leaderboardService.recalculateAllBoards(true);
            realtimeEventPublisher.publish(RealtimeEventType.EQUIPMENT_CHANGED, buildEquipmentPayload("delete", id));
        }
        return removed;
    }

    @Override
    @Transactional
    public boolean updateViewCount(Long id) {
        // Use atomic SQL increment to avoid lost-update race conditions under concurrency.
        return this.baseMapper.incrViewCount(id) > 0;
    }

    @Override
    @Transactional
    public boolean updateFavoriteCount(Long id, Integer delta) {
        // GREATEST(0, ...) guard is enforced in the SQL itself.
        return this.baseMapper.incrFavoriteCount(id, delta == null ? 0 : delta) > 0;
    }

    @Override
    public List<Equipment> getHotEquipment(Integer count) {
        return this.list(new LambdaQueryWrapper<Equipment>()
            .eq(Equipment::getStatus, 1)
            .orderByDesc(Equipment::getViewCount)
            .orderByDesc(Equipment::getFavoriteCount)
            .last("LIMIT " + count));
    }

    @Override
    public List<Equipment> searchEquipment(String keyword, Long categoryId, String brand, Double minPrice, Double maxPrice, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<Equipment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Equipment::getStatus, 1);
        
        if (categoryId != null) {
            wrapper.eq(Equipment::getCategoryId, categoryId);
        }
        
        if (brand != null && !brand.isEmpty()) {
            wrapper.eq(Equipment::getBrand, brand);
        }
        
        if (minPrice != null) {
            wrapper.ge(Equipment::getPrice, minPrice);
        }
        
        if (maxPrice != null) {
            wrapper.le(Equipment::getPrice, maxPrice);
        }
        
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w
                .like(Equipment::getName, keyword)
                .or()
                .like(Equipment::getBrand, keyword)
                .or()
                .like(Equipment::getModel, keyword)
                .or()
                .like(Equipment::getDescription, keyword)
                .or()
                .like(Equipment::getFeatures, keyword)
                .or()
                .like(Equipment::getSuitableFor, keyword)
            );
        }
        
        Page<Equipment> page = new Page<>(pageNum, pageSize);
        return this.page(page, wrapper).getRecords();
    }

    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        Long total = this.count();
        Long online = this.count(new LambdaQueryWrapper<Equipment>().eq(Equipment::getStatus, 1));
        Long recommend = this.count(new LambdaQueryWrapper<Equipment>().eq(Equipment::getIsRecommend, 1));
        
        stats.put("total", total);
        stats.put("online", online);
        stats.put("recommend", recommend);
        
        return stats;
    }

    private void normalizeImageFields(Equipment equipment) {
        List<String> imageUrls = parseImageUrls(equipment.getImages());
        if (equipment.getMainImage() != null && !equipment.getMainImage().trim().isEmpty()) {
            String mainImage = equipment.getMainImage().trim();
            if (!imageUrls.contains(mainImage)) {
                imageUrls.add(0, mainImage);
            }
        }

        imageUrls = imageUrls.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .distinct()
            .collect(java.util.stream.Collectors.toList());

        if (imageUrls.isEmpty()) {
            equipment.setImages(null);
            equipment.setMainImage(null);
            return;
        }

        if (equipment.getMainImage() == null || equipment.getMainImage().trim().isEmpty()) {
            equipment.setMainImage(imageUrls.get(0));
        } else {
            equipment.setMainImage(equipment.getMainImage().trim());
        }
        equipment.setImages(JSONUtil.toJsonStr(imageUrls));
    }

    private List<String> parseImageUrls(String rawImages) {
        if (rawImages == null || rawImages.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String trimmed = rawImages.trim();
        if (JSONUtil.isTypeJSONArray(trimmed)) {
            JSONArray array = JSONUtil.parseArray(trimmed);
            List<String> result = new ArrayList<>();
            for (Object item : array) {
                if (item != null) {
                    result.add(String.valueOf(item));
                }
            }
            return result;
        }

        return Arrays.stream(trimmed.split(","))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .collect(java.util.stream.Collectors.toList());
    }

    private int defaultInteger(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 递归获取分类及其所有子分类的 ID
     */
    private List<Long> getAllCategoryIds(Long categoryId) {
        List<Long> result = new ArrayList<>();
        result.add(categoryId);
        
        // 查询直接子分类
        List<Category> children = categoryMapper.selectList(
            new LambdaQueryWrapper<Category>()
                .eq(Category::getParentId, categoryId)
        );
        
        // 递归获取子分类的子分类
        for (Category child : children) {
            result.addAll(getAllCategoryIds(child.getId()));
        }
        
        return result;
    }

    private Map<String, Object> buildEquipmentPayload(String action, Long equipmentId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", action);
        payload.put("equipmentId", equipmentId);
        payload.put("scope", "equipment");
        return payload;
    }
}
