package com.pingpong.dto;

import lombok.Data;

@Data
public class LoginContextDTO {
    private Boolean rememberMe;
    private String ipAddress;
    private String userAgent;
    private String deviceFingerprint;
    private String deviceName;
}
