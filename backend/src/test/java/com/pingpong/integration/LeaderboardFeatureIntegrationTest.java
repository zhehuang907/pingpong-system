package com.pingpong.integration;

import com.jayway.jsonpath.JsonPath;
import com.pingpong.PingpongRecommendationApplication;
import com.pingpong.service.LeaderboardService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = PingpongRecommendationApplication.class)
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LeaderboardFeatureIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LeaderboardService leaderboardService;

    @Test
    void leaderboardEndpointsShouldBeReachableAfterRecalculation() throws Exception {
        assertDoesNotThrow(() -> leaderboardService.recalculateAllBoards(false));

        String detailJson = mockMvc.perform(get("/leaderboard/detail/weekly_popular")
                .param("sortBy", "score")
                .param("limit", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.boardType").value("weekly_popular"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        mockMvc.perform(get("/leaderboard/home").param("limit", "4"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.length()").value(Matchers.greaterThanOrEqualTo(1)));

        Integer equipmentId = JsonPath.read(detailJson, "$.data.items[0].equipmentId");
        mockMvc.perform(get("/leaderboard/equipment/" + equipmentId + "/positions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.length()").value(Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    void shareEndpointsShouldRecordAndExposeStatistics() throws Exception {
        assertDoesNotThrow(() -> leaderboardService.recalculateAllBoards(false));

        mockMvc.perform(post("/leaderboard/share")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"boardType\":\"weekly_popular\",\"channel\":\"wechat\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.boardType").value("weekly_popular"));

        mockMvc.perform(get("/leaderboard/share/stats").param("boardType", "weekly_popular"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.totalShares").value(Matchers.greaterThanOrEqualTo(1)));
    }
}
