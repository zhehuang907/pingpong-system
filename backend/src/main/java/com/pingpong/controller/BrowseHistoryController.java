package com.pingpong.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pingpong.common.Result;
import com.pingpong.dto.BrowseHistoryRecordDTO;
import com.pingpong.entity.BrowseHistory;
import com.pingpong.entity.Equipment;
import com.pingpong.exception.BusinessException;
import com.pingpong.mapper.BrowseHistoryMapper;
import com.pingpong.mapper.EquipmentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/history")
public class BrowseHistoryController {

    @Autowired
    private BrowseHistoryMapper browseHistoryMapper;

    @Autowired
    private EquipmentMapper equipmentMapper;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @PostMapping("/record/{equipmentId}")
    public Result<Boolean> recordBrowseLegacy(@PathVariable Long equipmentId, HttpServletRequest request) {
        BrowseHistoryRecordDTO dto = new BrowseHistoryRecordDTO();
        dto.setEquipmentId(equipmentId);
        dto.setContentType("equipment_detail");
        dto.setEnterTime(LocalDateTime.now());
        dto.setLeaveTime(LocalDateTime.now());
        dto.setDurationSeconds(0);
        dto.setInteractionActions(new ArrayList<>());
        return recordBrowse(dto, request);
    }

    @PostMapping("/record")
    public Result<Boolean> recordBrowse(@RequestBody BrowseHistoryRecordDTO requestBody, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.success(false);
        }

        if (requestBody == null || requestBody.getEquipmentId() == null) {
            throw new BusinessException("浏览记录缺少器材信息");
        }

        Equipment equipment = equipmentMapper.selectById(requestBody.getEquipmentId());
        if (equipment == null) {
            throw new BusinessException("浏览内容不存在");
        }

        BrowseHistory history = loadOrCreateHistory(userId, requestBody);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime enterTime = coalesce(requestBody.getEnterTime(), history.getEnterTime(), history.getCreateTime(), now);
        LocalDateTime leaveTime = coalesce(requestBody.getLeaveTime(), history.getLeaveTime(), now);
        if (leaveTime.isBefore(enterTime)) {
            leaveTime = enterTime;
        }

        history.setUserId(userId);
        history.setEquipmentId(requestBody.getEquipmentId());
        history.setContentType(defaultString(requestBody.getContentType(), history.getContentType(), "equipment_detail"));
        history.setSourcePage(defaultString(requestBody.getSourcePage(), history.getSourcePage(), "equipment"));
        history.setSessionId(defaultString(requestBody.getSessionId(), history.getSessionId(), null));
        history.setEnterTime(enterTime);
        history.setLeaveTime(leaveTime);
        history.setContentSnapshot(defaultString(requestBody.getContentSnapshot(), history.getContentSnapshot(), equipment.getName()));

        Set<String> mergedActions = new LinkedHashSet<>(parseActions(history.getInteractionActions()));
        mergedActions.addAll(requestBody.getInteractionActions() == null ? new ArrayList<>() : requestBody.getInteractionActions());
        history.setInteractionActions(JSONUtil.toJsonStr(new ArrayList<>(mergedActions)));

        int interactionCount = Math.max(
            safeInt(requestBody.getInteractionCount()),
            mergedActions.size()
        );
        history.setInteractionCount(Math.max(interactionCount, safeInt(history.getInteractionCount())));

        int durationSeconds = Math.max(
            safeInt(requestBody.getDurationSeconds()),
            calculateDurationSeconds(enterTime, leaveTime)
        );
        history.setDurationSeconds(Math.max(durationSeconds, safeInt(history.getDurationSeconds())));

        if (history.getId() == null) {
            browseHistoryMapper.insert(history);
        } else {
            browseHistoryMapper.updateById(history);
        }

        clearPersonalRecommendCache(userId);
        return Result.success(true);
    }

    @GetMapping("/list")
    public Result<List<BrowseHistory>> getUserHistory(
            HttpServletRequest request,
            @RequestParam(defaultValue = "20") Integer limit,
            @RequestParam(defaultValue = "false") Boolean deduplicate) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.success(new ArrayList<>());
        }

        List<BrowseHistory> list = Boolean.TRUE.equals(deduplicate)
            ? browseHistoryMapper.selectLatestUniqueHistory(userId, Math.max(limit, 1))
            : browseHistoryMapper.selectRecentHistory(userId, Math.max(limit, 1));

        attachEquipment(list);
        return Result.success(list);
    }

    @DeleteMapping("/{historyId}")
    public Result<Boolean> deleteHistoryItem(@PathVariable Long historyId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        BrowseHistory history = browseHistoryMapper.selectById(historyId);
        if (history == null || !Objects.equals(history.getUserId(), userId)) {
            throw new BusinessException("浏览记录不存在");
        }
        clearPersonalRecommendCache(userId);
        return Result.success(browseHistoryMapper.deleteById(historyId) > 0);
    }

    @DeleteMapping("/clear")
    public Result<Boolean> clearHistory(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        int deleted = browseHistoryMapper.delete(
            new LambdaQueryWrapper<BrowseHistory>()
                .eq(BrowseHistory::getUserId, userId)
        );
        clearPersonalRecommendCache(userId);
        return Result.success(deleted >= 0);
    }

    private BrowseHistory loadOrCreateHistory(Long userId, BrowseHistoryRecordDTO requestBody) {
        if (StringUtils.hasText(requestBody.getSessionId())) {
            BrowseHistory existing = browseHistoryMapper.selectLatestBySession(userId, requestBody.getEquipmentId(), requestBody.getSessionId());
            if (existing != null) {
                return existing;
            }
        }
        return new BrowseHistory();
    }

    private void attachEquipment(List<BrowseHistory> list) {
        if (list == null || list.isEmpty()) {
            return;
        }

        Set<Long> equipmentIds = list.stream()
            .map(BrowseHistory::getEquipmentId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        Map<Long, Equipment> equipmentMap = equipmentIds.isEmpty()
            ? java.util.Collections.emptyMap()
            : equipmentMapper.selectBatchIds(equipmentIds).stream()
                .collect(Collectors.toMap(Equipment::getId, equipment -> equipment, (left, right) -> left));

        for (BrowseHistory history : list) {
            Equipment equipment = equipmentMap.get(history.getEquipmentId());
            history.setEquipment(equipment);
            history.setEquipmentName(equipment == null ? null : equipment.getName());
            history.setInteractionActionList(parseActions(history.getInteractionActions()));
        }
    }

    private List<String> parseActions(String actionJson) {
        if (!StringUtils.hasText(actionJson)) {
            return new ArrayList<>();
        }
        try {
            return JSONUtil.toList(JSONUtil.parseArray(actionJson), String.class);
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    private int calculateDurationSeconds(LocalDateTime enterTime, LocalDateTime leaveTime) {
        if (enterTime == null || leaveTime == null) {
            return 0;
        }
        return (int) Math.max(Duration.between(enterTime, leaveTime).getSeconds(), 0);
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : Math.max(value, 0);
    }

    private LocalDateTime coalesce(LocalDateTime... values) {
        for (LocalDateTime value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String defaultString(String preferred, String fallback, String lastChoice) {
        if (StringUtils.hasText(preferred)) {
            return preferred.trim();
        }
        if (StringUtils.hasText(fallback)) {
            return fallback;
        }
        return lastChoice;
    }

    private void clearPersonalRecommendCache(Long userId) {
        if (userId == null || redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.delete("recommend:personal:" + userId);
        } catch (Exception ignored) {
        }
    }
}
