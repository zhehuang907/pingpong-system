package com.pingpong.controller;

import com.pingpong.common.AdminAccessHelper;
import com.pingpong.common.Result;
import com.pingpong.service.AdminDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/admin/dashboard")
public class AdminDashboardController {

    @Autowired
    private AdminAccessHelper adminAccessHelper;

    @Autowired
    private AdminDashboardService adminDashboardService;

    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview(HttpServletRequest request) {
        adminAccessHelper.requireAdmin(request);
        return Result.success(adminDashboardService.getOverview());
    }

    @GetMapping("/export")
    public ResponseEntity<ByteArrayResource> exportOverview(HttpServletRequest request) {
        adminAccessHelper.requireAdmin(request);
        byte[] bytes = adminDashboardService.exportOverviewReport();
        ByteArrayResource resource = new ByteArrayResource(bytes);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=admin-dashboard-overview.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(bytes.length)
                .body(resource);
    }
}
