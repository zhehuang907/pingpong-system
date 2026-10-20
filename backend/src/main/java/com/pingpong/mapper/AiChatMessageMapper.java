package com.pingpong.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pingpong.entity.AiChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AiChatMessageMapper extends BaseMapper<AiChatMessage> {

    @Select("SELECT * FROM ai_chat_message " +
            "WHERE session_id = #{sessionId} AND deleted = 0 " +
            "ORDER BY id ASC")
    List<AiChatMessage> selectSessionMessages(@Param("sessionId") Long sessionId);

    @Select("SELECT * FROM (" +
            "SELECT * FROM ai_chat_message " +
            "WHERE session_id = #{sessionId} AND deleted = 0 " +
            "ORDER BY id DESC LIMIT #{limit}" +
            ") recent_messages ORDER BY id ASC")
    List<AiChatMessage> selectRecentContextMessages(@Param("sessionId") Long sessionId, @Param("limit") Integer limit);
}
