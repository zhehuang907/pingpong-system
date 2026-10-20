package com.pingpong.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pingpong.common.AdminAccessHelper;
import com.pingpong.common.Result;
import com.pingpong.entity.Equipment;
import com.pingpong.service.EquipmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/equipment")
public class EquipmentController {

    @Autowired
    private EquipmentService equipmentService;

    @Autowired
    private AdminAccessHelper adminAccessHelper;

    @GetMapping("/list")
    public Result<Page<Equipment>> getEquipmentList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String sortBy) {
        Page<Equipment> page = equipmentService.getEquipmentList(pageNum, pageSize, categoryId, keyword, minPrice, maxPrice, sortBy);
        return Result.success(page);
    }

    @GetMapping("/detail/{id}")
    public Result<Equipment> getEquipmentDetail(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "true") Boolean trackView) {
        Equipment equipment = equipmentService.getEquipmentDetail(id, Boolean.TRUE.equals(trackView));
        if (equipment == null) {
            return Result.error(404, "装备不存在或已下架");
        }
        return Result.success(equipment);
    }

    @GetMapping("/search")
    public Result<List<Equipment>> searchEquipment(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        List<Equipment> list = equipmentService.searchEquipment(keyword, categoryId, brand, minPrice, maxPrice, pageNum, pageSize);
        return Result.success(list);
    }

    @PostMapping
    public Result<Boolean> addEquipment(@RequestBody Equipment equipment, HttpServletRequest request) {
        adminAccessHelper.requireAdmin(request);
        boolean result = equipmentService.addEquipment(equipment);
        return Result.success(result);
    }

    @PutMapping
    public Result<Boolean> updateEquipment(@RequestBody Equipment equipment, HttpServletRequest request) {
        adminAccessHelper.requireAdmin(request);
        boolean result = equipmentService.updateEquipment(equipment);
        return Result.success(result);
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> deleteEquipment(@PathVariable Long id, HttpServletRequest request) {
        adminAccessHelper.requireAdmin(request);
        boolean result = equipmentService.deleteEquipment(id);
        return Result.success(result);
    }

    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = equipmentService.getStatistics();
        return Result.success(stats);
    }
}
