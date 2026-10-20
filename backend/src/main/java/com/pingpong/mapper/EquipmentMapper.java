package com.pingpong.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pingpong.entity.Equipment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface EquipmentMapper extends BaseMapper<Equipment> {

    /**
     * 原子递增浏览次数 — 避免先查后改导致的并发计数丢失
     */
    @Update("UPDATE equipment SET view_count = view_count + 1 WHERE id = #{id} AND deleted = 0")
    int incrViewCount(@Param("id") Long id);

    /**
     * 原子递增/递减收藏次数（delta 传 1 或 -1）
     * 使用 GREATEST 保证不低于 0
     */
    @Update("UPDATE equipment SET favorite_count = GREATEST(0, favorite_count + #{delta}) WHERE id = #{id} AND deleted = 0")
    int incrFavoriteCount(@Param("id") Long id, @Param("delta") int delta);

    /**
     * 推荐候选池查询 — 排除已交互装备，限制数量，避免 selectList(null) 全表扫描
     */
    @Select({
        "<script>",
        "SELECT * FROM equipment",
        "WHERE status = 1 AND deleted = 0",
        "<if test='excludeIds != null and excludeIds.size() > 0'>",
        "  AND id NOT IN",
        "  <foreach item='id' collection='excludeIds' open='(' separator=',' close=')'>",
        "    #{id}",
        "  </foreach>",
        "</if>",
        "ORDER BY view_count DESC, favorite_count DESC",
        "LIMIT #{limit}",
        "</script>"
    })
    List<Equipment> selectRecommendCandidates(
            @Param("excludeIds") List<Long> excludeIds,
            @Param("limit") int limit);
}
