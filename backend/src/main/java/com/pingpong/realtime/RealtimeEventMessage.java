package com.pingpong.realtime;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class RealtimeEventMessage {
    private String type;
    private LocalDateTime timestamp;
    private Map<String, Object> payload;
}
