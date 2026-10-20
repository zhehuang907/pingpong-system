package com.pingpong.scheduler;

import com.pingpong.service.LeaderboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LeaderboardScheduler {

    @Autowired
    private LeaderboardService leaderboardService;

    @Scheduled(cron = "0 0 2 * * ?")
    public void fullRecalculate() {
        leaderboardService.recalculateAllBoards(false);
    }

    @Scheduled(cron = "0 0 0/6 * * ?")
    public void incrementalRecalculate() {
        leaderboardService.recalculateAllBoards(true);
    }
}
