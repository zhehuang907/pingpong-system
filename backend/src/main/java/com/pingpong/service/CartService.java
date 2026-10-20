package com.pingpong.service;

import com.pingpong.entity.CartItem;

import java.util.List;
import java.util.Map;

public interface CartService {

    Map<String, Object> addToCart(Long userId, Long equipmentId, Integer quantity);

    List<CartItem> getUserCart(Long userId);

    Map<String, Object> updateQuantity(Long userId, Long cartItemId, Integer quantity);

    Map<String, Object> updateSelected(Long userId, Long cartItemId, Boolean selected);

    Map<String, Object> updateSelectedAll(Long userId, Boolean selected);

    boolean removeCartItem(Long userId, Long cartItemId);

    boolean clearCart(Long userId);

    long getCartCount(Long userId);

    Map<String, Object> getCartSummary(Long userId);
}
