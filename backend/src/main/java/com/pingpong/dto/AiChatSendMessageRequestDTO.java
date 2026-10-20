package com.pingpong.dto;

import lombok.Data;

@Data
public class AiChatSendMessageRequestDTO {
    private Long sessionId;
    private String content;
    private Long equipmentId;
}
