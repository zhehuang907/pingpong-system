package com.pingpong.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pingpong.entity.CommentLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CommentLikeMapper extends BaseMapper<CommentLike> {

    @Select("SELECT id, user_id, comment_id, create_time, update_time, deleted " +
        "FROM comment_like WHERE user_id = #{userId} AND comment_id = #{commentId} LIMIT 1")
    CommentLike selectAnyByUserIdAndCommentId(@Param("userId") Long userId, @Param("commentId") Long commentId);

    @Update("UPDATE comment_like SET deleted = #{deleted}, update_time = NOW() WHERE id = #{id}")
    int updateDeletedState(@Param("id") Long id, @Param("deleted") Integer deleted);
}
