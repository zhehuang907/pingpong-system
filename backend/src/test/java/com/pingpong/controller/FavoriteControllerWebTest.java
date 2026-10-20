package com.pingpong.controller;

import com.pingpong.service.FavoriteService;
import com.pingpong.service.impl.FavoriteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FavoriteControllerWebTest {

    @Mock
    private FavoriteService favoriteService;

    @InjectMocks
    private FavoriteController favoriteController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(favoriteController, "favoriteService", favoriteService);
        mockMvc = MockMvcBuilders.standaloneSetup(favoriteController).build();
    }

    @Test
    void toggleFavoriteShouldReturnLatestFavoriteState() throws Exception {
        FavoriteServiceImpl.FavoriteToggleResult result = new FavoriteServiceImpl.FavoriteToggleResult(false, 8);
        when(favoriteService.toggleFavorite(1L, 10L)).thenReturn(result);

        mockMvc.perform(post("/favorite/toggle/10").requestAttr("userId", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.success").value(true))
            .andExpect(jsonPath("$.data.isFavorite").value(false))
            .andExpect(jsonPath("$.data.favoriteCount").value(8));
    }
}
