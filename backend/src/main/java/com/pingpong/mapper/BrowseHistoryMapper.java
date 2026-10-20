package com.pingpong.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pingpong.entity.BrowseHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BrowseHistoryMapper extends BaseMapper<BrowseHistory> {

    @Select("SELECT * FROM browse_history " +
            "WHERE user_id = #{userId} AND session_id = #{sessionId} AND equipment_id = #{equipmentId} AND deleted = 0 " +
            "ORDER BY id DESC LIMIT 1")
    BrowseHistory selectLatestBySession(@Param("userId") Long userId,
                                        @Param("equipmentId") Long equipmentId,
                                        @Param("sessionId") String sessionId);

    @Select("SELECT h1.* FROM browse_history h1 " +
            "JOIN (SELECT MAX(id) AS max_id FROM browse_history WHERE user_id = #{userId} AND deleted = 0 GROUP BY equipment_id) h2 " +
            "ON h1.id = h2.max_id " +
            "ORDER BY h1.update_time DESC, h1.create_time DESC " +
            "LIMIT #{limit}")
    List<BrowseHistory> selectLatestUniqueHistory(@Param("userId") Long userId, @Param("limit") Integer limit);

    @Select("SELECT * FROM browse_history " +
            "WHERE user_id = #{userId} AND deleted = 0 " +
            "ORDER BY update_time DESC, create_time DESC " +
            "LIMIT #{limit}")
    List<BrowseHistory> selectRecentHistory(@Param("userId") Long userId, @Param("limit") Integer limit);
}
