package com.pingpong.controller;

import com.pingpong.common.AdminAccessHelper;
import com.pingpong.config.GlobalExceptionHandler;
import com.pingpong.service.AdminDashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminDashboardControllerWebTest {

    @Mock
    private AdminAccessHelper adminAccessHelper;

    @Mock
    private AdminDashboardService adminDashboardService;

    @InjectMocks
    private AdminDashboardController adminDashboardController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adminDashboardController, "adminAccessHelper", adminAccessHelper);
        ReflectionTestUtils.setField(adminDashboardController, "adminDashboardService", adminDashboardService);
        mockMvc = MockMvcBuilders.standaloneSetup(adminDashboardController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void overviewShouldRejectNonAdmin() throws Exception {
        doThrow(new RuntimeException("无权执行后台操作")).when(adminAccessHelper).requireAdmin(any());

        mockMvc.perform(get("/admin/dashboard/overview").requestAttr("userId", 2L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500))
            .andExpect(jsonPath("$.msg").value("无权执行后台操作"));
    }

    @Test
    void overviewShouldReturnDashboardData() throws Exception {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalUsers", 12);

        Map<String, Object> overview = new HashMap<>();
        overview.put("summary", summary);
        overview.put("userGrowthTrend", Collections.emptyList());

        when(adminAccessHelper.requireAdmin(any())).thenReturn(1L);
        when(adminDashboardService.getOverview()).thenReturn(overview);

        mockMvc.perform(get("/admin/dashboard/overview").requestAttr("userId", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.summary.totalUsers").value(12));
    }
}
