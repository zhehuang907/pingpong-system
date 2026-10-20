package com.pingpong.service;

import com.pingpong.dto.AiChatCreateSessionRequestDTO;
import com.pingpong.dto.AiChatSendMessageRequestDTO;
import com.pingpong.dto.AiChatSendMessageResponseDTO;
import com.pingpong.dto.AiChatSessionDetailDTO;
import com.pingpong.dto.AiChatSessionSummaryDTO;
import com.pingpong.dto.AiChatUpdateSessionRequestDTO;

import java.util.List;

public interface AiChatService {
    List<AiChatSessionSummaryDTO> listSessions(Long userId);

    AiChatSessionSummaryDTO createSession(Long userId, AiChatCreateSessionRequestDTO request);

    AiChatSessionDetailDTO getSessionDetail(Long userId, Long sessionId);

    AiChatSessionSummaryDTO updateSession(Long userId, Long sessionId, AiChatUpdateSessionRequestDTO request);

    boolean deleteSession(Long userId, Long sessionId);

    AiChatSendMessageResponseDTO sendMessage(Long userId, AiChatSendMessageRequestDTO request);
}
