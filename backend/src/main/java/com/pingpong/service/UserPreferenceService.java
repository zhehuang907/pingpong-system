package com.pingpong.service;

import com.pingpong.dto.UserPreferenceProfileDTO;
import com.pingpong.entity.UserPreferenceProfile;

public interface UserPreferenceService {

    UserPreferenceProfile getByUserId(Long userId);

    UserPreferenceProfile saveOrUpdate(Long userId, UserPreferenceProfileDTO request);
}
