package com.pingpong.service.impl;

import com.pingpong.entity.Equipment;
import com.pingpong.entity.Favorite;
import com.pingpong.mapper.EquipmentMapper;
import com.pingpong.mapper.FavoriteMapper;
import com.pingpong.service.EquipmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceImplTest {

    @Mock
    private FavoriteMapper favoriteMapper;

    @Mock
    private EquipmentMapper equipmentMapper;

    @Mock
    private EquipmentService equipmentService;

    @InjectMocks
    private FavoriteServiceImpl favoriteService;

    @Test
    void toggleFavoriteShouldRemoveExistingFavorite() {
        Favorite existing = new Favorite();
        existing.setId(5L);
        when(equipmentMapper.selectById(99L)).thenReturn(new Equipment());
        when(favoriteMapper.selectOne(any())).thenReturn(existing);
        when(favoriteMapper.deleteById(5L)).thenReturn(1);

        FavoriteServiceImpl.FavoriteToggleResult result = favoriteService.toggleFavorite(1L, 99L);

        assertFalse(result.isFavorite());
        verify(equipmentService).updateFavoriteCount(99L, -1);
    }

    @Test
    void toggleFavoriteShouldCreateFavoriteWhenNotExists() {
        when(equipmentMapper.selectById(99L)).thenReturn(new Equipment());
        when(favoriteMapper.selectOne(any())).thenReturn(null);
        when(favoriteMapper.insert(any(Favorite.class))).thenReturn(1);

        FavoriteServiceImpl.FavoriteToggleResult result = favoriteService.toggleFavorite(1L, 99L);

        assertTrue(result.isFavorite());
        ArgumentCaptor<Favorite> captor = ArgumentCaptor.forClass(Favorite.class);
        verify(favoriteMapper).insert(captor.capture());
        verify(equipmentService).updateFavoriteCount(99L, 1);
        assertTrue(captor.getValue().getUserId().equals(1L));
        assertTrue(captor.getValue().getEquipmentId().equals(99L));
    }

    @Test
    void getUserFavoritesShouldPopulateEquipmentDetails() {
        Favorite favorite = new Favorite();
        favorite.setEquipmentId(99L);
        when(favoriteMapper.selectList(any())).thenReturn(Collections.singletonList(favorite));

        Equipment equipment = new Equipment();
        equipment.setId(99L);
        when(equipmentMapper.selectBatchIds(anyCollection())).thenReturn(Collections.singletonList(equipment));

        List<Favorite> favorites = favoriteService.getUserFavorites(1L);

        assertSame(equipment, favorites.get(0).getEquipment());
    }
}
