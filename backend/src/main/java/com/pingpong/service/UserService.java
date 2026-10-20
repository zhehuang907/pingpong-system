package com.pingpong.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pingpong.dto.LoginContextDTO;
import com.pingpong.dto.LoginDTO;
import com.pingpong.dto.LoginResponseDTO;
import com.pingpong.dto.RegisterDTO;
import com.pingpong.entity.User;

public interface UserService extends IService<User> {

    String register(RegisterDTO registerDTO);

    LoginResponseDTO login(LoginDTO loginDTO, LoginContextDTO loginContext);

    User getUserInfo(Long userId);

    boolean updateUserInfo(Long userId, User user);

    boolean changePassword(Long userId, String oldPassword, String newPassword);

    boolean resetPassword(String email, String newPassword);

    boolean checkUsernameExists(String username);

    boolean checkEmailExists(String email);
}
