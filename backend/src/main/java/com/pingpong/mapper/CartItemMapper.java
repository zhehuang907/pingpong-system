package com.pingpong.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pingpong.entity.CartItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CartItemMapper extends BaseMapper<CartItem> {
    @Select("SELECT COALESCE(SUM(quantity), 0) FROM shopping_cart WHERE user_id = #{userId} AND deleted = 0")
    long sumQuantityByUserId(Long userId);

    @Select("SELECT id, user_id, equipment_id, quantity, selected, create_time, update_time, deleted " +
        "FROM shopping_cart WHERE user_id = #{userId} AND equipment_id = #{equipmentId} LIMIT 1")
    CartItem selectAnyByUserIdAndEquipmentId(@Param("userId") Long userId, @Param("equipmentId") Long equipmentId);

    @Update("UPDATE shopping_cart SET deleted = 0, quantity = #{quantity}, selected = #{selected}, update_time = NOW() " +
        "WHERE id = #{id}")
    int reviveDeletedItem(@Param("id") Long id, @Param("quantity") Integer quantity, @Param("selected") Integer selected);
}
