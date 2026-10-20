package com.pingpong.controller;

import com.pingpong.common.Result;
import com.pingpong.dto.AiChatCreateSessionRequestDTO;
import com.pingpong.dto.AiChatSendMessageRequestDTO;
import com.pingpong.dto.AiChatSendMessageResponseDTO;
import com.pingpong.dto.AiChatSessionDetailDTO;
import com.pingpong.dto.AiChatSessionSummaryDTO;
import com.pingpong.dto.AiChatUpdateSessionRequestDTO;
import com.pingpong.exception.BusinessException;
import com.pingpong.service.AiChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/ai-chat")
public class AiChatController {

    @Autowired
    private AiChatService aiChatService;

    @GetMapping("/sessions")
    public Result<List<AiChatSessionSummaryDTO>> listSessions(HttpServletRequest request) {
        return Result.success(aiChatService.listSessions(requireUserId(request)));
    }

    @PostMapping("/sessions")
    public Result<AiChatSessionSummaryDTO> createSession(@RequestBody(required = false) AiChatCreateSessionRequestDTO request,
                                                         HttpServletRequest httpServletRequest) {
        return Result.success(aiChatService.createSession(requireUserId(httpServletRequest), request));
    }

    @GetMapping("/sessions/{sessionId}")
    public Result<AiChatSessionDetailDTO> getSessionDetail(@PathVariable Long sessionId,
                                                           HttpServletRequest request) {
        return Result.success(aiChatService.getSessionDetail(requireUserId(request), sessionId));
    }

    @PutMapping("/sessions/{sessionId}")
    public Result<AiChatSessionSummaryDTO> updateSession(@PathVariable Long sessionId,
                                                         @RequestBody AiChatUpdateSessionRequestDTO request,
                                                         HttpServletRequest httpServletRequest) {
        return Result.success(aiChatService.updateSession(requireUserId(httpServletRequest), sessionId, request));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public Result<Boolean> deleteSession(@PathVariable Long sessionId, HttpServletRequest request) {
        return Result.success(aiChatService.deleteSession(requireUserId(request), sessionId));
    }

    @PostMapping("/messages")
    public Result<AiChatSendMessageResponseDTO> sendMessage(@RequestBody AiChatSendMessageRequestDTO request,
                                                            HttpServletRequest httpServletRequest) {
        return Result.success(aiChatService.sendMessage(requireUserId(httpServletRequest), request));
    }

    private Long requireUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        return userId;
    }
}
