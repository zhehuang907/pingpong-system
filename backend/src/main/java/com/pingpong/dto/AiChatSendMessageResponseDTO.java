package com.pingpong.dto;

import lombok.Data;

@Data
public class AiChatSendMessageResponseDTO {
    private AiChatSessionSummaryDTO session;
    private AiChatMessageDTO userMessage;
    private AiChatMessageDTO assistantMessage;
}
