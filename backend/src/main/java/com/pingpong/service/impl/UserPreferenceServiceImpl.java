package com.pingpong.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pingpong.dto.UserPreferenceProfileDTO;
import com.pingpong.entity.User;
import com.pingpong.entity.UserPreferenceProfile;
import com.pingpong.mapper.UserMapper;
import com.pingpong.mapper.UserPreferenceProfileMapper;
import com.pingpong.service.UserPreferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;

@Service
public class UserPreferenceServiceImpl implements UserPreferenceService {

    @Autowired
    private UserPreferenceProfileMapper userPreferenceProfileMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public UserPreferenceProfile getByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        return userPreferenceProfileMapper.selectOne(
            new LambdaQueryWrapper<UserPreferenceProfile>()
                .eq(UserPreferenceProfile::getUserId, userId)
                .last("LIMIT 1")
        );
    }

    @Override
    @Transactional
    public UserPreferenceProfile saveOrUpdate(Long userId, UserPreferenceProfileDTO request) {
        UserPreferenceProfile profile = getByUserId(userId);
        if (profile == null) {
            profile = new UserPreferenceProfile();
            profile.setUserId(userId);
        }

        profile.setLevel(trimToNull(request.getLevel()));
        profile.setPlayStyle(trimToNull(request.getPlayStyle()));
        profile.setGripStyle(trimToNull(request.getGripStyle()));
        profile.setBudgetMin(request.getBudgetMin());
        profile.setBudgetMax(request.getBudgetMax());
        profile.setWeeklyFrequency(trimToNull(request.getWeeklyFrequency()));
        profile.setPrimaryGoal(trimToNull(request.getPrimaryGoal()));
        profile.setPriorityFocus(trimToNull(request.getPriorityFocus()));
        profile.setPreferredCategories(toJson(request.getPreferredCategories()));
        profile.setPreferredBrands(toJson(request.getPreferredBrands()));
        profile.setPreferenceTags(toJson(request.getPreferenceTags()));
        profile.setAnswerSnapshot(toJson(request.getAnswerSnapshot()));
        profile.setUpdatedSource(StringUtils.hasText(request.getUpdatedSource()) ? request.getUpdatedSource() : "profile");

        if (profile.getId() == null) {
            userPreferenceProfileMapper.insert(profile);
        } else {
            userPreferenceProfileMapper.updateById(profile);
        }

        syncBaseUserProfile(userId, profile);
        return getByUserId(userId);
    }

    private void syncBaseUserProfile(Long userId, UserPreferenceProfile profile) {
        User user = new User();
        user.setId(userId);
        user.setLevel(profile.getLevel());
        user.setPlayStyle(profile.getPlayStyle());
        userMapper.updateById(user);
    }

    private String toJson(Object value) {
        Object safeValue = value == null ? Collections.emptyList() : value;
        return JSONUtil.toJsonStr(safeValue);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
