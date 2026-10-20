package com.pingpong.controller;

import com.pingpong.dto.LeaderboardBoardDTO;
import com.pingpong.dto.LeaderboardItemDTO;
import com.pingpong.dto.LeaderboardShareResponseDTO;
import com.pingpong.entity.LeaderboardSubscription;
import com.pingpong.service.LeaderboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LeaderboardControllerWebTest {

    @Mock
    private LeaderboardService leaderboardService;

    @InjectMocks
    private LeaderboardController leaderboardController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(leaderboardController, "leaderboardService", leaderboardService);
        mockMvc = MockMvcBuilders.standaloneSetup(leaderboardController).build();
    }

    @Test
    void getHomeLeaderboardsShouldReturnBoardData() throws Exception {
        LeaderboardItemDTO itemDTO = new LeaderboardItemDTO();
        itemDTO.setEquipmentId(10L);
        itemDTO.setName("蝴蝶经典底板");
        itemDTO.setRankPosition(1);
        itemDTO.setTotalScore(new BigDecimal("8.6"));

        LeaderboardBoardDTO boardDTO = new LeaderboardBoardDTO();
        boardDTO.setBoardType("weekly_popular");
        boardDTO.setBoardName("本周人气王");
        boardDTO.setItems(Collections.singletonList(itemDTO));

        when(leaderboardService.getHomeLeaderboards(6)).thenReturn(Collections.singletonList(boardDTO));

        mockMvc.perform(get("/leaderboard/home").param("limit", "6"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data[0].boardType").value("weekly_popular"))
            .andExpect(jsonPath("$.data[0].items[0].equipmentId").value(10));
    }

    @Test
    void getSubscriptionsShouldRejectAnonymousRequest() throws Exception {
        mockMvc.perform(get("/leaderboard/subscriptions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(401))
            .andExpect(jsonPath("$.msg").value("请先登录"));
    }

    @Test
    void subscribeShouldReturnSuccessForAuthenticatedUser() throws Exception {
        when(leaderboardService.subscribe(1L, "weekly_popular")).thenReturn(true);

        mockMvc.perform(post("/leaderboard/subscriptions/weekly_popular").requestAttr("userId", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void recordShareShouldReturnSharePayload() throws Exception {
        LeaderboardShareResponseDTO responseDTO = new LeaderboardShareResponseDTO();
        responseDTO.setBoardType("weekly_popular");
        responseDTO.setChannel("wechat");
        responseDTO.setTitle("本周人气王 | 乒乓装备口碑榜");
        responseDTO.setShareUrl("http://127.0.0.1:3000/leaderboards?boardType=weekly_popular");

        when(leaderboardService.recordShare(org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.any()))
            .thenReturn(responseDTO);

        mockMvc.perform(post("/leaderboard/share")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"boardType\":\"weekly_popular\",\"channel\":\"wechat\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.boardType").value("weekly_popular"))
            .andExpect(jsonPath("$.data.channel").value("wechat"));
    }
}
