package com.pingpong.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class LoginDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 20, message = "用户名长度不能超过 20 位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(max = 32, message = "密码长度不能超过 32 位")
    private String password;

    private Boolean rememberMe;

    @Size(max = 128, message = "设备指纹长度不能超过 128 位")
    private String deviceFingerprint;

    @Size(max = 64, message = "设备名称长度不能超过 64 位")
    private String deviceName;
}
