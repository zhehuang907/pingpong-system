package com.pingpong.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pingpong.dto.LoginAlertDTO;
import com.pingpong.dto.LoginContextDTO;
import com.pingpong.dto.LoginDTO;
import com.pingpong.dto.LoginResponseDTO;
import com.pingpong.dto.RegisterDTO;
import com.pingpong.entity.LoginAudit;
import com.pingpong.entity.User;
import com.pingpong.mapper.LoginAuditMapper;
import com.pingpong.mapper.UserMapper;
import com.pingpong.service.UserService;
import com.pingpong.exception.BusinessException;
import com.pingpong.util.CredentialPolicyUtil;
import com.pingpong.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final String RISK_LOW = "LOW";
    private static final String RISK_MEDIUM = "MEDIUM";
    private static final String RISK_HIGH = "HIGH";

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private LoginAuditMapper loginAuditMapper;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Value("${security.login.remember-me-expire-seconds:2592000}")
    private Long rememberMeExpireSeconds;

    @Value("${security.login.anomaly.failure-window-minutes:15}")
    private Integer failureWindowMinutes;

    @Value("${security.login.anomaly.failure-threshold:3}")
    private Integer failureThreshold;

    @Override
    @Transactional
    public String register(RegisterDTO registerDTO) {
        CredentialPolicyUtil.validateUsername(registerDTO.getUsername());
        CredentialPolicyUtil.validatePassword(registerDTO.getPassword());

        if (checkUsernameExists(registerDTO.getUsername())) {
            throw new BusinessException("用户名已存在");
        }

        if (StringUtils.hasText(registerDTO.getEmail()) && checkEmailExists(registerDTO.getEmail())) {
            throw new BusinessException("邮箱已被注册");
        }

        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(BCrypt.hashpw(registerDTO.getPassword()));
        user.setEmail(registerDTO.getEmail());
        user.setNickname(StringUtils.hasText(registerDTO.getNickname()) ? registerDTO.getNickname() : registerDTO.getUsername());
        user.setRole("USER");
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());

        this.save(user);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        cacheToken(user.getId(), token, jwtUtil.getExpireTime());
        return token;
    }

    @Override
    @Transactional
    public LoginResponseDTO login(LoginDTO loginDTO, LoginContextDTO loginContext) {
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, loginDTO.getUsername()));

        long recentFailureCount = countRecentFailedAttempts(loginDTO.getUsername());

        if (user == null) {
            String message = buildFailureMessage(recentFailureCount + 1);
            recordLoginAttempt(null, loginDTO.getUsername(), loginContext, false, riskLevelForFailure(recentFailureCount + 1), message);
            throw new RuntimeException(message);
        }

        if (!BCrypt.checkpw(loginDTO.getPassword(), user.getPassword())) {
            String message = buildFailureMessage(recentFailureCount + 1);
            recordLoginAttempt(user.getId(), user.getUsername(), loginContext, false, riskLevelForFailure(recentFailureCount + 1), message);
            throw new RuntimeException(message);
        }

        if (user.getStatus() == 0) {
            recordLoginAttempt(user.getId(), user.getUsername(), loginContext, false, RISK_MEDIUM, "账号已被禁用");
            throw new BusinessException("账号已被禁用");
        }

        List<LoginAlertDTO> alerts = buildLoginAlerts(user, loginContext, recentFailureCount);
        long expireSeconds = Boolean.TRUE.equals(loginContext.getRememberMe())
                ? rememberMeExpireSeconds
                : jwtUtil.getExpireTime();

        user.setLastLoginTime(LocalDateTime.now());
        this.updateById(user);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), expireSeconds);
        cacheToken(user.getId(), token, expireSeconds);
        recordLoginAttempt(
                user.getId(),
                user.getUsername(),
                loginContext,
                true,
                alerts.isEmpty() ? RISK_LOW : RISK_MEDIUM,
                alerts.stream().map(LoginAlertDTO::getMessage).collect(Collectors.joining(" | "))
        );

        LoginResponseDTO response = new LoginResponseDTO();
        response.setToken(token);
        response.setUsername(user.getUsername());
        response.setDisplayName(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
        response.setDeviceName(resolveDeviceName(loginContext));
        response.setRememberMe(Boolean.TRUE.equals(loginContext.getRememberMe()));
        response.setExpiresInSeconds(expireSeconds);
        response.setAlerts(alerts);
        return response;
    }

    @Override
    public User getUserInfo(Long userId) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(null);
        return user;
    }

    @Override
    @Transactional
    public boolean updateUserInfo(Long userId, User user) {
        user.setId(userId);
        user.setPassword(null);
        return this.updateById(user);
    }

    @Override
    @Transactional
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        CredentialPolicyUtil.validatePassword(newPassword);

        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (!BCrypt.checkpw(oldPassword, user.getPassword())) {
            throw new RuntimeException("原密码不正确");
        }

        user.setPassword(BCrypt.hashpw(newPassword));
        return this.updateById(user);
    }

    @Override
    @Transactional
    public boolean resetPassword(String email, String newPassword) {
        CredentialPolicyUtil.validatePassword(newPassword);

        User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {
            throw new RuntimeException("邮箱尚未注册");
        }

        user.setPassword(BCrypt.hashpw(newPassword));
        return this.updateById(user);
    }

    @Override
    public boolean checkUsernameExists(String username) {
        return this.count(new LambdaQueryWrapper<User>().eq(User::getUsername, username)) > 0;
    }

    @Override
    public boolean checkEmailExists(String email) {
        return this.count(new LambdaQueryWrapper<User>().eq(User::getEmail, email)) > 0;
    }

    private List<LoginAlertDTO> buildLoginAlerts(User user, LoginContextDTO loginContext, long recentFailureCount) {
        List<LoginAlertDTO> alerts = new ArrayList<>();
        LoginAudit lastSuccess = findLastSuccessfulLogin(user.getId());

        if (recentFailureCount >= failureThreshold) {
            alerts.add(new LoginAlertDTO(
                    "RECENT_FAILURES",
                    "warning",
                    "检测到该账号近期存在多次失败登录，请确认刚才的尝试均为本人操作。"
            ));
        }

        if (lastSuccess != null
                && StringUtils.hasText(loginContext.getDeviceFingerprint())
                && StringUtils.hasText(lastSuccess.getDeviceFingerprint())
                && !Objects.equals(loginContext.getDeviceFingerprint(), lastSuccess.getDeviceFingerprint())) {
            alerts.add(new LoginAlertDTO(
                    "NEW_DEVICE",
                    "warning",
                    "本次登录设备与上次成功登录设备不同，请留意账号安全。"
            ));
        }

        if (lastSuccess != null
                && StringUtils.hasText(loginContext.getIpAddress())
                && StringUtils.hasText(lastSuccess.getIpAddress())
                && !Objects.equals(loginContext.getIpAddress(), lastSuccess.getIpAddress())) {
            alerts.add(new LoginAlertDTO(
                    "NETWORK_CHANGE",
                    "info",
                    "本次登录网络环境与上次不同，如非本人操作请尽快修改密码。"
            ));
        }

        return alerts;
    }

    private LoginAudit findLastSuccessfulLogin(Long userId) {
        return loginAuditMapper.selectOne(new LambdaQueryWrapper<LoginAudit>()
                .eq(LoginAudit::getUserId, userId)
                .eq(LoginAudit::getSuccess, 1)
                .orderByDesc(LoginAudit::getCreateTime)
                .last("LIMIT 1"));
    }

    private long countRecentFailedAttempts(String username) {
        LocalDateTime thresholdTime = LocalDateTime.now().minusMinutes(failureWindowMinutes);
        return loginAuditMapper.selectCount(new LambdaQueryWrapper<LoginAudit>()
                .eq(LoginAudit::getUsername, username)
                .eq(LoginAudit::getSuccess, 0)
                .ge(LoginAudit::getCreateTime, thresholdTime));
    }

    private void recordLoginAttempt(
            Long userId,
            String username,
            LoginContextDTO loginContext,
            boolean success,
            String riskLevel,
            String warningMessage
    ) {
        try {
            LoginAudit audit = new LoginAudit();
            audit.setUserId(userId);
            audit.setUsername(username);
            audit.setSuccess(success ? 1 : 0);
            audit.setIpAddress(trimToLength(loginContext.getIpAddress(), 64));
            audit.setUserAgent(trimToLength(loginContext.getUserAgent(), 255));
            audit.setDeviceFingerprint(trimToLength(loginContext.getDeviceFingerprint(), 128));
            audit.setDeviceName(trimToLength(resolveDeviceName(loginContext), 64));
            audit.setRiskLevel(trimToLength(riskLevel, 16));
            audit.setWarningMessage(trimToLength(warningMessage, 255));
            audit.setCreateTime(LocalDateTime.now());
            TransactionTemplate auditTransaction = new TransactionTemplate(transactionManager);
            auditTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            auditTransaction.executeWithoutResult(status -> {
                loginAuditMapper.insert(audit);
            });
        } catch (Exception ignored) {
            // Login should not fail only because audit logging is unavailable.
        }
    }

    private String resolveDeviceName(LoginContextDTO loginContext) {
        if (StringUtils.hasText(loginContext.getDeviceName())) {
            return loginContext.getDeviceName();
        }
        if (StringUtils.hasText(loginContext.getUserAgent())) {
            return trimToLength(loginContext.getUserAgent(), 64);
        }
        return "Unknown Device";
    }

    private String buildFailureMessage(long failedAttempts) {
        if (failedAttempts >= failureThreshold) {
            return "检测到连续登录失败，请确认是否本人操作，必要时请及时修改密码。";
        }
        return "用户名或密码错误";
    }

    private String riskLevelForFailure(long failedAttempts) {
        if (failedAttempts >= failureThreshold + 2) {
            return RISK_HIGH;
        }
        if (failedAttempts >= failureThreshold) {
            return RISK_MEDIUM;
        }
        return RISK_LOW;
    }

    private void cacheToken(Long userId, String token, long expireSeconds) {
        try {
            redisTemplate.opsForValue().set(
                    "user:token:" + userId,
                    token,
                    expireSeconds,
                    TimeUnit.SECONDS
            );
        } catch (Exception ignored) {
            // Local development and offline modes may not provide Redis.
        }
    }

    private String trimToLength(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
