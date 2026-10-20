package com.pingpong.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class RegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Pattern(
        regexp = "^[A-Za-z][A-Za-z0-9_]{5,19}$",
        message = "用户名需以字母开头，长度 6 到 20 位，仅允许字母、数字和下划线"
    )
    private String username;

    @NotBlank(message = "密码不能为空")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{10,32}$",
        message = "密码需为 10 到 32 位，且包含大小写字母、数字和特殊字符"
    )
    private String password;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过 100 位")
    private String email;

    @Size(max = 20, message = "昵称长度不能超过 20 位")
    private String nickname;
}
