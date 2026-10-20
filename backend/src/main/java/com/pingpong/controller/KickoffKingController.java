package com.pingpong.controller;

import com.pingpong.common.Result;
import com.pingpong.dto.KickoffKingOverviewDTO;
import com.pingpong.service.KickoffKingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping({"/kaiqiuwang-ranking", "/kickoff-king"})
public class KickoffKingController {

    @Autowired
    private KickoffKingService kickoffKingService;

    @GetMapping("/overview")
    public Result<KickoffKingOverviewDTO> getOverview(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(kickoffKingService.getOverview(userId));
    }
}
