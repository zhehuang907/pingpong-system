package com.pingpong.common;

import com.pingpong.entity.User;
import com.pingpong.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component
public class AdminAccessHelper {

    @Autowired
    private UserMapper userMapper;

    public Long requireAdmin(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new RuntimeException("请先登录");
        }

        User currentUser = userMapper.selectById(userId);
        if (currentUser == null || currentUser.getStatus() == null || currentUser.getStatus() != 1) {
            throw new RuntimeException("当前账号不可用");
        }

        if (!"ADMIN".equalsIgnoreCase(currentUser.getRole())) {
            throw new RuntimeException("无权执行后台操作");
        }

        return userId;
    }
}
