package com.pingpong.service;

import com.pingpong.dto.KickoffKingOverviewDTO;

public interface KickoffKingService {

    KickoffKingOverviewDTO getOverview(Long userId);
}
