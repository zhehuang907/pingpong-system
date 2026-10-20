package com.pingpong.integration;

import com.pingpong.PingpongRecommendationApplication;
import com.pingpong.service.AdminDashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = PingpongRecommendationApplication.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AdminDashboardOverviewIntegrationTest {

    @Autowired
    private AdminDashboardService adminDashboardService;

    @Test
    void overviewShouldLoadFromRuntimeDatabase() {
        Map<String, Object> overview = adminDashboardService.getOverview();
        assertNotNull(overview);
        assertTrue(overview.containsKey("summary"));
    }
}
