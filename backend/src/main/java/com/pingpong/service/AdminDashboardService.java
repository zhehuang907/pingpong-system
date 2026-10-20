package com.pingpong.service;

import java.util.Map;

public interface AdminDashboardService {

    Map<String, Object> getOverview();

    byte[] exportOverviewReport();
}
