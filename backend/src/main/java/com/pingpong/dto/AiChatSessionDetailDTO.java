package com.pingpong.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiChatSessionDetailDTO {
    private AiChatSessionSummaryDTO session;
    private List<AiChatMessageDTO> messages;
}
