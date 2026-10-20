package com.pingpong.service.impl;

import com.pingpong.entity.CartItem;
import com.pingpong.entity.Equipment;
import com.pingpong.mapper.CartItemMapper;
import com.pingpong.mapper.EquipmentMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartItemMapper cartItemMapper;

    @Mock
    private EquipmentMapper equipmentMapper;

    @InjectMocks
    private CartServiceImpl cartService;

    @Test
    void addToCartShouldReviveDeletedItemInsteadOfInsertingDuplicate() {
        Equipment equipment = buildEquipment(10L, 20);
        CartItem deletedItem = new CartItem();
        deletedItem.setId(99L);
        deletedItem.setUserId(1L);
        deletedItem.setEquipmentId(10L);
        deletedItem.setQuantity(4);
        deletedItem.setDeleted(1);

        when(equipmentMapper.selectById(10L)).thenReturn(equipment);
        when(cartItemMapper.selectAnyByUserIdAndEquipmentId(1L, 10L)).thenReturn(deletedItem);
        when(cartItemMapper.selectList(any())).thenReturn(Collections.singletonList(deletedItem));
        when(cartItemMapper.sumQuantityByUserId(1L)).thenReturn(3L);
        when(equipmentMapper.selectBatchIds(anyCollection())).thenReturn(Collections.singletonList(equipment));

        Map<String, Object> payload = cartService.addToCart(1L, 10L, 3);

        verify(cartItemMapper).reviveDeletedItem(99L, 3, 1);
        verify(cartItemMapper, never()).insert(any(CartItem.class));
        assertEquals(3, ((CartItem) payload.get("cartItem")).getQuantity());
        assertEquals(3L, payload.get("cartCount"));
    }

    @Test
    void addToCartShouldMergeQuantityAfterConcurrentDuplicateInsert() {
        Equipment equipment = buildEquipment(10L, 20);
        CartItem concurrentItem = new CartItem();
        concurrentItem.setId(88L);
        concurrentItem.setUserId(1L);
        concurrentItem.setEquipmentId(10L);
        concurrentItem.setQuantity(2);
        concurrentItem.setSelected(1);
        concurrentItem.setDeleted(0);

        when(equipmentMapper.selectById(10L)).thenReturn(equipment);
        when(cartItemMapper.selectAnyByUserIdAndEquipmentId(1L, 10L)).thenReturn(null, concurrentItem);
        when(cartItemMapper.insert(any(CartItem.class))).thenThrow(new DuplicateKeyException("duplicate cart row"));
        when(cartItemMapper.selectList(any())).thenReturn(Collections.singletonList(concurrentItem));
        when(cartItemMapper.sumQuantityByUserId(1L)).thenReturn(4L);
        when(equipmentMapper.selectBatchIds(anyCollection())).thenReturn(Collections.singletonList(equipment));

        Map<String, Object> payload = cartService.addToCart(1L, 10L, 2);

        ArgumentCaptor<CartItem> updateCaptor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemMapper).updateById(updateCaptor.capture());
        assertEquals(4, updateCaptor.getValue().getQuantity());
        assertEquals(4, ((CartItem) payload.get("cartItem")).getQuantity());
        assertEquals(4L, payload.get("cartCount"));
    }

    private Equipment buildEquipment(Long id, int stock) {
        Equipment equipment = new Equipment();
        equipment.setId(id);
        equipment.setStock(stock);
        equipment.setStatus(1);
        equipment.setPrice(BigDecimal.TEN);
        return equipment;
    }
}
