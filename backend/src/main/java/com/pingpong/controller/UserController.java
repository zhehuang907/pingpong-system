package com.pingpong.controller;

import com.pingpong.common.Result;
import com.pingpong.dto.LoginContextDTO;
import com.pingpong.dto.LoginDTO;
import com.pingpong.dto.LoginResponseDTO;
import com.pingpong.dto.RegisterDTO;
import com.pingpong.dto.UserPreferenceProfileDTO;
import com.pingpong.entity.User;
import com.pingpong.entity.UserPreferenceProfile;
import com.pingpong.exception.BusinessException;
import com.pingpong.service.ImageStorageService;
import com.pingpong.service.UserPreferenceService;
import com.pingpong.service.UserService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserPreferenceService userPreferenceService;

    @Autowired
    private ImageStorageService imageStorageService;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @PostMapping("/register")
    public Result<String> register(@Valid @RequestBody RegisterDTO registerDTO) {
        String token = userService.register(registerDTO);
        return Result.success("Register success", token);
    }

    @PostMapping("/login")
    public Result<LoginResponseDTO> login(@Valid @RequestBody LoginDTO loginDTO, HttpServletRequest request) {
        LoginContextDTO context = new LoginContextDTO();
        context.setRememberMe(loginDTO.getRememberMe());
        context.setDeviceFingerprint(loginDTO.getDeviceFingerprint());
        context.setDeviceName(loginDTO.getDeviceName());
        context.setIpAddress(resolveClientIp(request));
        context.setUserAgent(request.getHeader("User-Agent"));
        LoginResponseDTO response = userService.login(loginDTO, context);
        return Result.success("Login success", response);
    }

    @GetMapping("/info")
    public Result<User> getUserInfo(HttpServletRequest request) {
        Long userId = requireUserId(request);
        User user = userService.getUserInfo(userId);
        return Result.success(user);
    }

    @PutMapping("/info")
    public Result<Boolean> updateUserInfo(@RequestBody User user, HttpServletRequest request) {
        Long userId = requireUserId(request);
        boolean result = userService.updateUserInfo(userId, user);
        clearPersonalRecommendCache(userId);
        return Result.success(result);
    }

    @PostMapping("/avatar")
    public Result<User> uploadAvatar(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws IOException {
        Long userId = requireUserId(request);
        Map<String, Object> uploadResult = imageStorageService.storeImage(file, "avatar");

        User update = new User();
        update.setAvatar((String) uploadResult.get("url"));
        userService.updateUserInfo(userId, update);
        clearPersonalRecommendCache(userId);

        return Result.success(userService.getUserInfo(userId));
    }

    @GetMapping("/preferences")
    public Result<UserPreferenceProfile> getUserPreferences(HttpServletRequest request) {
        Long userId = requireUserId(request);
        return Result.success(userPreferenceService.getByUserId(userId));
    }

    @PutMapping("/preferences")
    public Result<UserPreferenceProfile> updateUserPreferences(
            @RequestBody UserPreferenceProfileDTO requestBody,
            HttpServletRequest request) {
        Long userId = requireUserId(request);
        UserPreferenceProfile profile = userPreferenceService.saveOrUpdate(userId, requestBody);
        clearPersonalRecommendCache(userId);
        return Result.success(profile);
    }

    @PutMapping("/password")
    public Result<Boolean> changePassword(@RequestBody PasswordChangeDTO passwordChangeDTO, HttpServletRequest request) {
        Long userId = requireUserId(request);
        boolean result = userService.changePassword(userId, passwordChangeDTO.getOldPassword(), passwordChangeDTO.getNewPassword());
        return Result.success(result);
    }

    @PostMapping("/forgot-password")
    public Result<Boolean> resetPassword(@RequestBody ResetPasswordDTO resetPasswordDTO) {
        boolean result = userService.resetPassword(resetPasswordDTO.getEmail(), resetPasswordDTO.getNewPassword());
        return Result.success(result);
    }

    @GetMapping("/check-username")
    public Result<Boolean> checkUsername(@RequestParam String username) {
        boolean exists = userService.checkUsernameExists(username);
        return Result.success(!exists);
    }

    @GetMapping("/check-email")
    public Result<Boolean> checkEmail(@RequestParam String email) {
        boolean exists = userService.checkEmailExists(email);
        return Result.success(!exists);
    }

    @Data
    static class PasswordChangeDTO {
        private String oldPassword;
        private String newPassword;
    }

    @Data
    static class ResetPasswordDTO {
        private String email;
        private String newPassword;
    }

    private Long requireUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(401, "Not logged in or session expired");
        }
        return userId;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.trim().isEmpty()) {
            String[] addresses = forwardedFor.split(",");
            if (addresses.length > 0 && !addresses[0].trim().isEmpty()) {
                return addresses[0].trim();
            }
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.trim().isEmpty()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

    private void clearPersonalRecommendCache(Long userId) {
        if (userId == null || redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.delete("recommend:personal:" + userId);
        } catch (Exception ignored) {
        }
    }
}
