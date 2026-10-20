package com.pingpong.service;

import com.pingpong.dto.EquipmentLeaderboardPositionDTO;
import com.pingpong.dto.LeaderboardBoardDTO;
import com.pingpong.dto.LeaderboardShareRequestDTO;
import com.pingpong.dto.LeaderboardShareResponseDTO;
import com.pingpong.entity.LeaderboardSubscription;

import java.util.List;
import java.util.Map;

public interface LeaderboardService {

    void recalculateAllBoards(boolean incremental);

    List<LeaderboardBoardDTO> getHomeLeaderboards(Integer limit);

    LeaderboardBoardDTO getLeaderboardDetail(String boardType, String sortBy, Integer limit);

    List<EquipmentLeaderboardPositionDTO> getEquipmentRankings(Long equipmentId);

    List<LeaderboardSubscription> getUserSubscriptions(Long userId);

    boolean subscribe(Long userId, String boardType);

    boolean unsubscribe(Long userId, String boardType);

    LeaderboardShareResponseDTO recordShare(Long userId, LeaderboardShareRequestDTO requestDTO);

    Map<String, Object> getShareStatistics(String boardType);
}
