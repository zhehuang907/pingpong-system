package com.pingpong.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pingpong.entity.BrowseHistory;
import com.pingpong.entity.Comment;
import com.pingpong.entity.Equipment;
import com.pingpong.entity.Favorite;
import com.pingpong.entity.LoginAudit;
import com.pingpong.entity.User;
import com.pingpong.mapper.BrowseHistoryMapper;
import com.pingpong.mapper.CommentMapper;
import com.pingpong.mapper.EquipmentMapper;
import com.pingpong.mapper.FavoriteMapper;
import com.pingpong.mapper.LoginAuditMapper;
import com.pingpong.mapper.UserMapper;
import com.pingpong.service.AdminDashboardService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private EquipmentMapper equipmentMapper;

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private BrowseHistoryMapper browseHistoryMapper;

    @Autowired
    private LoginAuditMapper loginAuditMapper;

    @Override
    public Map<String, Object> getOverview() {
        LocalDate startDate = LocalDate.now().minusDays(6);
        LocalDateTime startTime = startDate.atStartOfDay();

        Map<String, Object> overview = new HashMap<>();
        overview.put("summary", buildSummary(startTime));
        overview.put("userGrowthTrend", buildUserGrowthTrend(startDate, startTime));
        overview.put("accessTrend", buildAccessTrend(startDate, startTime));
        overview.put("playStyleDistribution", buildPlayStyleDistribution());
        overview.put("topEquipments", buildTopEquipments());
        return overview;
    }

    @Override
    @SuppressWarnings("unchecked")
    public byte[] exportOverviewReport() {
        Map<String, Object> overview = getOverview();
        Map<String, Object> summary = (Map<String, Object>) overview.get("summary");
        List<Map<String, Object>> topEquipments = (List<Map<String, Object>>) overview.get("topEquipments");
        List<Map<String, Object>> userGrowthTrend = (List<Map<String, Object>>) overview.get("userGrowthTrend");
        List<Map<String, Object>> accessTrend = (List<Map<String, Object>>) overview.get("accessTrend");

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet summarySheet = workbook.createSheet("Summary");
            writeSummarySheet(summarySheet, summary);

            Sheet topSheet = workbook.createSheet("TopEquipments");
            writeTopEquipmentSheet(topSheet, topEquipments);

            Sheet trendSheet = workbook.createSheet("Trends");
            writeTrendSheet(trendSheet, userGrowthTrend, accessTrend);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to export dashboard report", ex);
        }
    }

    private Map<String, Object> buildSummary(LocalDateTime startTime) {
        Map<String, Object> summary = new HashMap<>();

        long totalUsers = userMapper.selectCount(new LambdaQueryWrapper<User>());
        long activeUsers7d = loginAuditMapper.selectList(new LambdaQueryWrapper<LoginAudit>()
                .select(LoginAudit::getUserId)
                .eq(LoginAudit::getSuccess, 1)
                .ge(LoginAudit::getCreateTime, startTime)).stream()
            .map(LoginAudit::getUserId)
            .filter(Objects::nonNull)
            .distinct()
            .count();
        long totalComments = commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getStatus, 1));
        long pendingComments = commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getStatus, 1)
                .eq(Comment::getReviewStatus, 0));
        long approvedComments = commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getStatus, 1)
                .eq(Comment::getReviewStatus, 1));
        long rejectedComments = commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getStatus, 1)
                .eq(Comment::getReviewStatus, 2));
        long totalEquipments = equipmentMapper.selectCount(new LambdaQueryWrapper<Equipment>());
        long onlineEquipments = equipmentMapper.selectCount(new LambdaQueryWrapper<Equipment>()
                .eq(Equipment::getStatus, 1));
        long totalFavorites = favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>());

        List<Equipment> equipmentStats = equipmentMapper.selectList(new LambdaQueryWrapper<Equipment>()
            .select(Equipment::getViewCount));
        long totalViews = equipmentStats.stream()
            .map(Equipment::getViewCount)
            .filter(Objects::nonNull)
            .mapToLong(Integer::longValue)
            .sum();

        summary.put("totalUsers", totalUsers);
        summary.put("activeUsers7d", activeUsers7d);
        summary.put("totalComments", totalComments);
        summary.put("pendingComments", pendingComments);
        summary.put("approvedComments", approvedComments);
        summary.put("rejectedComments", rejectedComments);
        summary.put("totalEquipments", totalEquipments);
        summary.put("onlineEquipments", onlineEquipments);
        summary.put("totalFavorites", totalFavorites);
        summary.put("totalViews", totalViews);
        return summary;
    }

    private List<Map<String, Object>> buildUserGrowthTrend(LocalDate startDate, LocalDateTime startTime) {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
            .select(User::getCreateTime)
            .ge(User::getCreateTime, startTime));

        Map<LocalDate, Long> countByDate = users.stream()
            .filter(user -> user.getCreateTime() != null)
            .collect(Collectors.groupingBy(
                user -> user.getCreateTime().toLocalDate(),
                Collectors.counting()
            ));

        return buildDailySeries(startDate, date -> {
            Map<String, Object> point = new HashMap<>();
            point.put("date", date.format(DATE_FORMATTER));
            point.put("value", countByDate.getOrDefault(date, 0L));
            return point;
        });
    }

    private List<Map<String, Object>> buildAccessTrend(LocalDate startDate, LocalDateTime startTime) {
        List<BrowseHistory> histories = browseHistoryMapper.selectList(new LambdaQueryWrapper<BrowseHistory>()
            .select(BrowseHistory::getUserId, BrowseHistory::getCreateTime)
            .ge(BrowseHistory::getCreateTime, startTime));

        Map<LocalDate, Long> pvCount = new HashMap<>();
        Map<LocalDate, Set<Long>> uvCount = new HashMap<>();

        for (BrowseHistory history : histories) {
            if (history.getCreateTime() == null) {
                continue;
            }
            LocalDate date = history.getCreateTime().toLocalDate();
            pvCount.merge(date, 1L, (a, b) -> a + b);
            uvCount.computeIfAbsent(date, key -> new HashSet<>());
            if (history.getUserId() != null) {
                uvCount.get(date).add(history.getUserId());
            }
        }

        return buildDailySeries(startDate, date -> {
            Map<String, Object> point = new HashMap<>();
            point.put("date", date.format(DATE_FORMATTER));
            point.put("pv", pvCount.getOrDefault(date, 0L));
            point.put("uv", (long) uvCount.getOrDefault(date, new HashSet<>()).size());
            return point;
        });
    }

    private List<Map<String, Object>> buildPlayStyleDistribution() {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
            .select(User::getPlayStyle));

        Map<String, Long> distribution = users.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(user -> {
                if (!StringUtils.hasText(user.getPlayStyle())) {
                    return "未填写";
                }
                return user.getPlayStyle().trim();
            }, Collectors.counting()));

        List<Map<String, Object>> result = new ArrayList<>();
        distribution.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .forEach(entry -> {
                Map<String, Object> item = new HashMap<>();
                item.put("name", entry.getKey());
                item.put("value", entry.getValue());
                result.add(item);
            });
        return result;
    }

    private List<Map<String, Object>> buildTopEquipments() {
        List<Equipment> equipments = equipmentMapper.selectList(new LambdaQueryWrapper<Equipment>()
            .select(
                Equipment::getId,
                Equipment::getName,
                Equipment::getBrand,
                Equipment::getMainImage,
                Equipment::getViewCount,
                Equipment::getFavoriteCount,
                Equipment::getRating,
                Equipment::getStatus
            )
            .eq(Equipment::getStatus, 1)
            .orderByDesc(Equipment::getFavoriteCount)
            .orderByDesc(Equipment::getViewCount)
            .orderByDesc(Equipment::getRating)
            .last("LIMIT 10"));

        List<Map<String, Object>> result = new ArrayList<>();
        int rank = 1;
        for (Equipment equipment : equipments) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("rank", rank++);
            item.put("id", equipment.getId());
            item.put("name", equipment.getName());
            item.put("brand", equipment.getBrand());
            item.put("mainImage", equipment.getMainImage());
            item.put("viewCount", defaultInteger(equipment.getViewCount()));
            item.put("favoriteCount", defaultInteger(equipment.getFavoriteCount()));
            item.put("rating", equipment.getRating() == null ? BigDecimal.ZERO : equipment.getRating());
            item.put("heatScore", calculateHeatScore(equipment));
            result.add(item);
        }
        return result;
    }

    private BigDecimal calculateHeatScore(Equipment equipment) {
        double score = defaultInteger(equipment.getFavoriteCount()) * 0.6
            + defaultInteger(equipment.getViewCount()) * 0.25
            + (equipment.getRating() == null ? 0D : equipment.getRating().doubleValue()) * 10D;
        return BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
    }

    private List<Map<String, Object>> buildDailySeries(LocalDate startDate, DailySeriesBuilder builder) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (int offset = 0; offset < 7; offset += 1) {
            result.add(builder.build(startDate.plusDays(offset)));
        }
        return result;
    }

    private int defaultInteger(Integer value) {
        return value == null ? 0 : value;
    }

    private void writeSummarySheet(Sheet sheet, Map<String, Object> summary) {
        if (summary == null || summary.isEmpty()) {
            return;
        }
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Metric");
        header.createCell(1).setCellValue("Value");

        int rowIndex = 1;
        for (Map.Entry<String, Object> entry : summary.entrySet()) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(entry.getKey());
            Cell valueCell = row.createCell(1);
            Object value = entry.getValue();
            if (value instanceof Number) {
                valueCell.setCellValue(((Number) value).doubleValue());
            } else {
                valueCell.setCellValue(value == null ? "" : String.valueOf(value));
            }
        }
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void writeTopEquipmentSheet(Sheet sheet, List<Map<String, Object>> topEquipments) {
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Rank");
        header.createCell(1).setCellValue("ID");
        header.createCell(2).setCellValue("Name");
        header.createCell(3).setCellValue("Brand");
        header.createCell(4).setCellValue("Views");
        header.createCell(5).setCellValue("Favorites");
        header.createCell(6).setCellValue("Rating");
        header.createCell(7).setCellValue("HeatScore");

        int rowIndex = 1;
        if (topEquipments != null) {
            for (Map<String, Object> item : topEquipments) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(valueAsDouble(item.get("rank")));
                row.createCell(1).setCellValue(valueAsDouble(item.get("id")));
                row.createCell(2).setCellValue(valueAsString(item.get("name")));
                row.createCell(3).setCellValue(valueAsString(item.get("brand")));
                row.createCell(4).setCellValue(valueAsDouble(item.get("viewCount")));
                row.createCell(5).setCellValue(valueAsDouble(item.get("favoriteCount")));
                row.createCell(6).setCellValue(valueAsDouble(item.get("rating")));
                row.createCell(7).setCellValue(valueAsDouble(item.get("heatScore")));
            }
        }
        for (int i = 0; i <= 7; i += 1) {
            sheet.autoSizeColumn(i);
        }
    }

    private void writeTrendSheet(Sheet sheet,
                                 List<Map<String, Object>> userGrowthTrend,
                                 List<Map<String, Object>> accessTrend) {
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Date");
        header.createCell(1).setCellValue("NewUsers");
        header.createCell(2).setCellValue("PV");
        header.createCell(3).setCellValue("UV");

        Map<String, Map<String, Object>> accessMap = new HashMap<>();
        if (accessTrend != null) {
            for (Map<String, Object> access : accessTrend) {
                accessMap.put(valueAsString(access.get("date")), access);
            }
        }

        int rowIndex = 1;
        if (userGrowthTrend != null) {
            for (Map<String, Object> growth : userGrowthTrend) {
                String date = valueAsString(growth.get("date"));
                Map<String, Object> access = accessMap.get(date);
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(date);
                row.createCell(1).setCellValue(valueAsDouble(growth.get("value")));
                row.createCell(2).setCellValue(valueAsDouble(access == null ? null : access.get("pv")));
                row.createCell(3).setCellValue(valueAsDouble(access == null ? null : access.get("uv")));
            }
        }

        for (int i = 0; i <= 3; i += 1) {
            sheet.autoSizeColumn(i);
        }
    }

    private String valueAsString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private double valueAsDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value == null) {
            return 0D;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return 0D;
        }
    }

    @FunctionalInterface
    private interface DailySeriesBuilder {
        Map<String, Object> build(LocalDate date);
    }
}
