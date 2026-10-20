package com.pingpong.service.impl;

import com.pingpong.entity.BrowseHistory;
import com.pingpong.entity.Comment;
import com.pingpong.entity.Equipment;
import com.pingpong.entity.Favorite;
import com.pingpong.mapper.BrowseHistoryMapper;
import com.pingpong.mapper.CommentMapper;
import com.pingpong.mapper.EquipmentMapper;
import com.pingpong.mapper.FavoriteMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.atLeastOnce;

@ExtendWith(MockitoExtension.class)
class RecommendServiceImplTest {

    @Mock
    private EquipmentMapper equipmentMapper;

    @Mock
    private BrowseHistoryMapper browseHistoryMapper;

    @Mock
    private FavoriteMapper favoriteMapper;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @InjectMocks
    private RecommendServiceImpl recommendService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(recommendService, "hotCount", 10);
        ReflectionTestUtils.setField(recommendService, "personalCount", 10);
        ReflectionTestUtils.setField(recommendService, "similarCount", 5);
        ReflectionTestUtils.setField(recommendService, "browseWeight", 1.0D);
        ReflectionTestUtils.setField(recommendService, "favoriteWeight", 3.0D);
        ReflectionTestUtils.setField(recommendService, "commentWeight", 2.0D);
        ReflectionTestUtils.setField(recommendService, "tagWeight", 0.4D);
        ReflectionTestUtils.setField(recommendService, "categoryWeight", 0.4D);
        ReflectionTestUtils.setField(recommendService, "priceWeight", 0.2D);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(redisTemplate.expire(anyString(), any(Long.class), any(TimeUnit.class))).thenReturn(true);
    }

    @Test
    void personalRecommendShouldPreferItemsSimilarToUserInteractions() {
        BrowseHistory history = new BrowseHistory();
        history.setEquipmentId(1L);
        when(browseHistoryMapper.selectList(any())).thenReturn(Collections.singletonList(history));
        when(favoriteMapper.selectList(any())).thenReturn(Collections.<Favorite>emptyList());
        when(commentMapper.selectList(any())).thenReturn(Collections.<Comment>emptyList());

        Equipment interacted = buildEquipment(1L, 1L, "Butterfly", new BigDecimal("100"), "[1,2]");
        Equipment similar = buildEquipment(2L, 1L, "Butterfly", new BigDecimal("110"), "[1,2]");
        Equipment different = buildEquipment(3L, 3L, "DHS", new BigDecimal("800"), "[9]");

        when(equipmentMapper.selectBatchIds(anyCollection())).thenReturn(Collections.singletonList(interacted));
        when(equipmentMapper.selectRecommendCandidates(anyList(), eq(300))).thenReturn(Arrays.asList(different, similar));

        List<Equipment> result = recommendService.getPersonalRecommend(7L, 2);

        assertEquals(Long.valueOf(2L), result.get(0).getId());
        verify(valueOperations).set(eq("recommend:personal:7"), anyList(), eq(30L), eq(TimeUnit.MINUTES));
        verify(hashOperations, atLeastOnce()).putAll(anyString(), anyMap());
    }

    private Equipment buildEquipment(Long id, Long categoryId, String brand, BigDecimal price, String tags) {
        Equipment equipment = new Equipment();
        equipment.setId(id);
        equipment.setCategoryId(categoryId);
        equipment.setBrand(brand);
        equipment.setPrice(price);
        equipment.setTags(tags);
        equipment.setStatus(1);
        equipment.setIsRecommend(0);
        equipment.setRating(new BigDecimal("4.00"));
        equipment.setViewCount(10);
        equipment.setFavoriteCount(5);
        equipment.setSalesCount(3);
        return equipment;
    }
}
