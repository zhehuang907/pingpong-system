package com.pingpong.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class BrowseHistoryRecordDTO {
    private Long equipmentId;
    private String contentType;
    private String sourcePage;
    private String sessionId;
    private LocalDateTime enterTime;
    private LocalDateTime leaveTime;
    private Integer durationSeconds;
    private Integer interactionCount;
    private List<String> interactionActions;
    private String contentSnapshot;
}
