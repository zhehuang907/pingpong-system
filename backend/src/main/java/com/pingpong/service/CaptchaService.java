package com.pingpong.service;

import com.pingpong.util.CaptchaUtil;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 验证码服务
 */
@Service
public class CaptchaService {

    // 使用 ConcurrentHashMap 存储验证码，key 为随机生成的 key，value 为验证码
    private static final Map<String, String> CAPTCHA_STORE = new ConcurrentHashMap<>();

    /**
     * 生成验证码并存储
     *
     * @param key 验证码 key
     * @return 验证码字符串
     */
    public String generateCaptcha(String key) {
        // 生成验证码
        String captchaCode = CaptchaUtil.generateCaptchaCode();

        // 如果 key 为空，使用时间戳作为 key
        if (key == null || key.isEmpty()) {
            key = String.valueOf(System.currentTimeMillis());
        }

        // 存储验证码，5 分钟后过期
        CAPTCHA_STORE.put(key, captchaCode);

        // 清理过期的验证码（简单实现，实际生产环境应该使用定时任务）
        if (CAPTCHA_STORE.size() > 1000) {
            CAPTCHA_STORE.clear();
        }

        return captchaCode;
    }

    /**
     * 获取验证码图片
     *
     * @param captchaCode 验证码字符串
     * @return 验证码图片
     */
    public BufferedImage getCaptchaImage(String captchaCode) {
        return CaptchaUtil.generateCaptchaImage(captchaCode);
    }

    /**
     * 验证验证码
     *
     * @param key  验证码 key
     * @param code 用户输入的验证码
     * @return 验证结果
     */
    public boolean validateCaptcha(String key, String code) {
        if (key == null || code == null) {
            return false;
        }

        // 获取存储的验证码
        String storedCode = CAPTCHA_STORE.get(key);

        if (storedCode == null) {
            return false;
        }

        // 比较验证码（忽略大小写）
        boolean valid = storedCode.equalsIgnoreCase(code);

        // 验证成功后删除验证码，防止重复使用
        if (valid) {
            CAPTCHA_STORE.remove(key);
        }

        return valid;
    }
}
