package com.pingpong;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.pingpong.mapper")
@EnableCaching
@EnableScheduling
public class PingpongRecommendationApplication {

    public static void main(String[] args) {
        SpringApplication.run(PingpongRecommendationApplication.class, args);
        System.out.println("========================================");
        System.out.println("  PingPong Recommendation System started");
        System.out.println("  URL: http://localhost:8081/api");
        System.out.println("========================================");
    }
}
