package com.pingpong.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pingpong.entity.Equipment;

import java.util.List;
import java.util.Map;

public interface EquipmentService extends IService<Equipment> {
    
    Page<Equipment> getEquipmentList(Integer pageNum, Integer pageSize, Long categoryId, String keyword, Double minPrice, Double maxPrice, String sortBy);
    
    Equipment getEquipmentDetail(Long id);

    Equipment getEquipmentDetail(Long id, boolean trackView);
    
    boolean addEquipment(Equipment equipment);
    
    boolean updateEquipment(Equipment equipment);
    
    boolean deleteEquipment(Long id);
    
    boolean updateViewCount(Long id);
    
    boolean updateFavoriteCount(Long id, Integer delta);
    
    List<Equipment> getHotEquipment(Integer count);
    
    List<Equipment> searchEquipment(String keyword, Long categoryId, String brand, Double minPrice, Double maxPrice, Integer pageNum, Integer pageSize);
    
    Map<String, Object> getStatistics();
}
