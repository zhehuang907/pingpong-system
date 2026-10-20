package com.pingpong.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai.deepseek")
public class DeepSeekProperties {
    private boolean enabled = false;
    private String baseUrl = "https://api.deepseek.com";
    private String chatPath = "/chat/completions";
    private String apiKey;
    private String model = "deepseek-chat";
    private Double temperature = 0.6D;
    private Integer maxOutputTokens = 1200;
    private Integer timeoutMillis = 30000;
    private Integer contextMessageLimit = 12;
    private Integer catalogLimit = 6;
}
