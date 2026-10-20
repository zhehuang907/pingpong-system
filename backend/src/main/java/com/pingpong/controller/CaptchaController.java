package com.pingpong.controller;

import com.pingpong.service.CaptchaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 验证码控制器
 */
@RestController
@RequestMapping("/captcha")
@CrossOrigin(origins = "*")
public class CaptchaController {

    @Autowired
    private CaptchaService captchaService;

    /**
     * 获取验证码图片
     */
    @GetMapping
    public void getCaptcha(@RequestParam(value = "k", required = false) String key, HttpServletResponse response) throws IOException {
        // 生成验证码
        String captchaCode = captchaService.generateCaptcha(key);

        // 生成验证码图片
        BufferedImage captchaImage = captchaService.getCaptchaImage(captchaCode);

        // 设置响应头
        response.setContentType("image/jpeg");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        // 输出图片
        ImageIO.write(captchaImage, "jpg", response.getOutputStream());
    }

    /**
     * 验证验证码
     */
    @GetMapping("/verify")
    public Map<String, Object> verifyCaptcha(@RequestParam String key, @RequestParam String code) {
        Map<String, Object> result = new HashMap<>();
        boolean valid = captchaService.validateCaptcha(key, code);

        if (valid) {
            result.put("code", 200);
            result.put("msg", "验证码正确");
            result.put("success", true);
        } else {
            result.put("code", 400);
            result.put("msg", "验证码错误");
            result.put("success", false);
        }

        return result;
    }
}
