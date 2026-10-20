package com.pingpong.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class UserProfileActionDTO {
    private String title;
    private String description;
    private String buttonText;
    private String targetPath;
    private Map<String, Object> query;
    private List<String> tags;
}
