package com.pingpong.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pingpong.dto.CartSummaryDTO;
import com.pingpong.entity.CartItem;
import com.pingpong.entity.Equipment;
import com.pingpong.mapper.CartItemMapper;
import com.pingpong.mapper.EquipmentMapper;
import com.pingpong.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartItemMapper cartItemMapper;

    @Autowired
    private EquipmentMapper equipmentMapper;

    @Override
    @Transactional
    public Map<String, Object> addToCart(Long userId, Long equipmentId, Integer quantity) {
        Equipment equipment = requireActiveEquipment(equipmentId);
        int requestedQuantity = Math.max(quantity == null ? 1 : quantity, 1);
        int maxAvailable = equipment.getStock() == null || equipment.getStock() < 0 ? requestedQuantity : equipment.getStock();

        CartItem existing = cartItemMapper.selectAnyByUserIdAndEquipmentId(userId, equipmentId);
        CartItem currentItem = existing != null
            ? mergeExistingCartItem(existing, requestedQuantity, maxAvailable)
            : insertNewCartItem(userId, equipmentId, requestedQuantity, maxAvailable);

        currentItem.setEquipment(equipment);
        return buildCartPayload(currentItem, userId);
    }

    @Override
    public List<CartItem> getUserCart(Long userId) {
        List<CartItem> cartItems = cartItemMapper.selectList(
            new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .orderByDesc(CartItem::getUpdateTime)
                .orderByDesc(CartItem::getCreateTime)
        );
        attachEquipment(cartItems);
        return cartItems;
    }

    @Override
    @Transactional
    public Map<String, Object> updateQuantity(Long userId, Long cartItemId, Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("购买数量至少为 1");
        }

        CartItem cartItem = findOwnedCartItem(userId, cartItemId);
        Equipment equipment = requireActiveEquipment(cartItem.getEquipmentId());

        int maxAvailable = equipment.getStock() == null || equipment.getStock() < 0 ? quantity : equipment.getStock();
        cartItem.setQuantity(Math.max(1, Math.min(quantity, Math.max(maxAvailable, 1))));
        cartItemMapper.updateById(cartItem);
        cartItem.setEquipment(equipment);
        return buildCartPayload(cartItem, userId);
    }

    @Override
    @Transactional
    public Map<String, Object> updateSelected(Long userId, Long cartItemId, Boolean selected) {
        CartItem cartItem = findOwnedCartItem(userId, cartItemId);
        cartItem.setSelected(Boolean.TRUE.equals(selected) ? 1 : 0);
        cartItemMapper.updateById(cartItem);
        return buildCartPayload(cartItem, userId);
    }

    @Override
    @Transactional
    public Map<String, Object> updateSelectedAll(Long userId, Boolean selected) {
        List<CartItem> cartItems = cartItemMapper.selectList(
            new LambdaQueryWrapper<CartItem>().eq(CartItem::getUserId, userId)
        );

        int selectedValue = Boolean.TRUE.equals(selected) ? 1 : 0;
        for (CartItem cartItem : cartItems) {
            if (!Objects.equals(cartItem.getSelected(), selectedValue)) {
                cartItem.setSelected(selectedValue);
                cartItemMapper.updateById(cartItem);
            }
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("summary", getCartSummary(userId).get("summary"));
        payload.put("cartCount", getCartCount(userId));
        payload.put("list", getUserCart(userId));
        return payload;
    }

    @Override
    @Transactional
    public boolean removeCartItem(Long userId, Long cartItemId) {
        CartItem cartItem = findOwnedCartItem(userId, cartItemId);
        return cartItemMapper.deleteById(cartItem.getId()) > 0;
    }

    @Override
    @Transactional
    public boolean clearCart(Long userId) {
        return cartItemMapper.delete(
            new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
        ) >= 0;
    }

    @Override
    public long getCartCount(Long userId) {
        return cartItemMapper.sumQuantityByUserId(userId);
    }

    @Override
    public Map<String, Object> getCartSummary(Long userId) {
        List<CartItem> cartItems = getUserCart(userId);
        CartSummaryDTO summary = buildSummary(cartItems);
        Map<String, Object> payload = new HashMap<>();
        payload.put("summary", summary);
        payload.put("cartCount", getCartCount(userId));
        return payload;
    }

    private void attachEquipment(List<CartItem> cartItems) {
        if (cartItems == null || cartItems.isEmpty()) {
            return;
        }

        Set<Long> equipmentIds = cartItems.stream()
            .map(CartItem::getEquipmentId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        if (equipmentIds.isEmpty()) {
            return;
        }

        Map<Long, Equipment> equipmentMap = equipmentMapper.selectBatchIds(equipmentIds).stream()
            .collect(Collectors.toMap(Equipment::getId, equipment -> equipment, (left, right) -> left));

        for (CartItem cartItem : cartItems) {
            cartItem.setEquipment(equipmentMap.get(cartItem.getEquipmentId()));
        }
    }

    private Equipment requireActiveEquipment(Long equipmentId) {
        Equipment equipment = equipmentMapper.selectById(equipmentId);
        if (equipment == null || equipment.getStatus() == null || equipment.getStatus() != 1) {
            throw new IllegalArgumentException("装备不存在或已下架");
        }
        return equipment;
    }

    private CartItem findOwnedCartItem(Long userId, Long cartItemId) {
        CartItem cartItem = cartItemMapper.selectById(cartItemId);
        if (cartItem == null || !Objects.equals(cartItem.getUserId(), userId)) {
            throw new IllegalArgumentException("购物车记录不存在");
        }
        return cartItem;
    }

    private CartItem mergeExistingCartItem(CartItem existing, int requestedQuantity, int maxAvailable) {
        int baseQuantity = defaultInteger(existing.getDeleted()) == 1 ? 0 : defaultInteger(existing.getQuantity());
        int mergedQuantity = maxAvailable > 0
            ? Math.min(baseQuantity + requestedQuantity, maxAvailable)
            : baseQuantity + requestedQuantity;
        existing.setQuantity(Math.max(mergedQuantity, 1));
        existing.setSelected(1);

        if (defaultInteger(existing.getDeleted()) == 1) {
            cartItemMapper.reviveDeletedItem(existing.getId(), existing.getQuantity(), existing.getSelected());
            existing.setDeleted(0);
        } else {
            cartItemMapper.updateById(existing);
        }
        return existing;
    }

    private CartItem insertNewCartItem(Long userId, Long equipmentId, int requestedQuantity, int maxAvailable) {
        CartItem cartItem = new CartItem();
        cartItem.setUserId(userId);
        cartItem.setEquipmentId(equipmentId);
        cartItem.setQuantity(maxAvailable > 0 ? Math.min(requestedQuantity, maxAvailable) : requestedQuantity);
        cartItem.setSelected(1);

        try {
            cartItemMapper.insert(cartItem);
            return cartItem;
        } catch (DuplicateKeyException ex) {
            CartItem concurrentItem = cartItemMapper.selectAnyByUserIdAndEquipmentId(userId, equipmentId);
            if (concurrentItem == null) {
                throw ex;
            }
            return mergeExistingCartItem(concurrentItem, requestedQuantity, maxAvailable);
        }
    }

    private Map<String, Object> buildCartPayload(CartItem cartItem, Long userId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("cartItem", cartItem);
        payload.put("cartCount", getCartCount(userId));
        payload.put("quantity", cartItem.getQuantity());
        payload.put("summary", getCartSummary(userId).get("summary"));
        return payload;
    }

    private int defaultInteger(Integer value) {
        return value == null ? 0 : value;
    }

    private CartSummaryDTO buildSummary(List<CartItem> cartItems) {
        CartSummaryDTO summary = new CartSummaryDTO();
        int itemCount = cartItems.size();
        int totalQuantity = 0;
        int selectedItemCount = 0;
        int selectedQuantity = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal selectedAmount = BigDecimal.ZERO;
        boolean allSelected = itemCount > 0;

        for (CartItem cartItem : cartItems) {
            int quantity = cartItem.getQuantity() == null ? 0 : cartItem.getQuantity();
            BigDecimal price = cartItem.getEquipment() == null || cartItem.getEquipment().getPrice() == null
                ? BigDecimal.ZERO
                : cartItem.getEquipment().getPrice();
            BigDecimal lineAmount = price.multiply(BigDecimal.valueOf(quantity));

            totalQuantity += quantity;
            totalAmount = totalAmount.add(lineAmount);

            if (Objects.equals(cartItem.getSelected(), 1)) {
                selectedItemCount += 1;
                selectedQuantity += quantity;
                selectedAmount = selectedAmount.add(lineAmount);
            } else {
                allSelected = false;
            }
        }

        summary.setItemCount(itemCount);
        summary.setTotalQuantity(totalQuantity);
        summary.setSelectedItemCount(selectedItemCount);
        summary.setSelectedQuantity(selectedQuantity);
        summary.setTotalAmount(totalAmount);
        summary.setSelectedAmount(selectedAmount);
        summary.setAllSelected(itemCount > 0 && allSelected);
        return summary;
    }
}
