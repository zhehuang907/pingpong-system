package com.pingpong.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pingpong.dto.LoginAlertDTO;
import com.pingpong.dto.LoginContextDTO;
import com.pingpong.dto.LoginDTO;
import com.pingpong.dto.LoginResponseDTO;
import com.pingpong.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerWebTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        ReflectionTestUtils.setField(userController, "userService", userService);
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setValidator(validator)
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void loginShouldReturnStructuredSecurityEnhancements() throws Exception {
        LoginResponseDTO responseDTO = new LoginResponseDTO();
        responseDTO.setToken("token-123");
        responseDTO.setUsername("PaddleAce_26");
        responseDTO.setDisplayName("旋转节奏");
        responseDTO.setDeviceName("Windows / 1920x1080");
        responseDTO.setRememberMe(true);
        responseDTO.setExpiresInSeconds(2592000L);
        responseDTO.setAlerts(Collections.singletonList(
                new LoginAlertDTO("NEW_DEVICE", "warning", "检测到新设备登录，请确认是否为本人操作")
        ));

        when(userService.login(any(LoginDTO.class), any(LoginContextDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "JUnit-Agent")
                        .header("X-Forwarded-For", "10.0.0.2")
                        .content("{\"username\":\"PaddleAce_26\",\"password\":\"PingPong@2026!A\",\"rememberMe\":true,"
                                + "\"deviceFingerprint\":\"fp_123456\",\"deviceName\":\"Windows / 1920x1080\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("token-123"))
                .andExpect(jsonPath("$.data.rememberMe").value(true))
                .andExpect(jsonPath("$.data.expiresInSeconds").value(2592000))
                .andExpect(jsonPath("$.data.alerts[0].type").value("NEW_DEVICE"));

        ArgumentCaptor<LoginContextDTO> contextCaptor = ArgumentCaptor.forClass(LoginContextDTO.class);
        verify(userService).login(any(LoginDTO.class), contextCaptor.capture());
        assertTrue(contextCaptor.getValue().getRememberMe());
        assertEquals("10.0.0.2", contextCaptor.getValue().getIpAddress());
        assertEquals("JUnit-Agent", contextCaptor.getValue().getUserAgent());
        assertEquals("fp_123456", contextCaptor.getValue().getDeviceFingerprint());
    }

    @Test
    void registerShouldRejectWeakPassword() throws Exception {
        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                "PaddleAce_26",
                                "weakpass",
                                "player@example.com",
                                "旋转节奏"
                        ))))
                .andExpect(status().isBadRequest());
    }

    private static class RegisterRequest {
        private final String username;
        private final String password;
        private final String email;
        private final String nickname;

        private RegisterRequest(String username, String password, String email, String nickname) {
            this.username = username;
            this.password = password;
            this.email = email;
            this.nickname = nickname;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public String getEmail() {
            return email;
        }

        public String getNickname() {
            return nickname;
        }
    }
}
