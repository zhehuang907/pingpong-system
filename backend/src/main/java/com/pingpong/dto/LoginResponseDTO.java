package com.pingpong.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class LoginResponseDTO {
    private String token;
    private String username;
    private String displayName;
    private String deviceName;
    private Boolean rememberMe;
    private Long expiresInSeconds;
    private List<LoginAlertDTO> alerts = new ArrayList<>();
}
