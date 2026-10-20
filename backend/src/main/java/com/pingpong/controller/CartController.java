package com.pingpong.controller;

import com.pingpong.common.Result;
import com.pingpong.entity.CartItem;
import com.pingpong.exception.BusinessException;
import com.pingpong.service.CartService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @PostMapping("/add/{equipmentId}")
    public Result<Map<String, Object>> addToCart(
            @PathVariable Long equipmentId,
            @RequestBody(required = false) AddCartRequest requestBody,
            HttpServletRequest request) {
        Long userId = requireUserId(request);
        Integer quantity = requestBody == null ? 1 : requestBody.getQuantity();
        return Result.success(cartService.addToCart(userId, equipmentId, quantity));
    }

    @GetMapping("/list")
    public Result<List<CartItem>> getUserCart(HttpServletRequest request) {
        Long userId = requireUserId(request);
        return Result.success(cartService.getUserCart(userId));
    }

    @GetMapping("/summary")
    public Result<Map<String, Object>> getCartSummary(HttpServletRequest request) {
        Long userId = requireUserId(request);
        return Result.success(cartService.getCartSummary(userId));
    }

    @PutMapping("/{cartItemId}/quantity")
    public Result<Map<String, Object>> updateQuantity(
            @PathVariable Long cartItemId,
            @Valid @RequestBody UpdateCartQuantityRequest requestBody,
            HttpServletRequest request) {
        Long userId = requireUserId(request);
        return Result.success(cartService.updateQuantity(userId, cartItemId, requestBody.getQuantity()));
    }

    @PutMapping("/{cartItemId}/selected")
    public Result<Map<String, Object>> updateSelected(
            @PathVariable Long cartItemId,
            @RequestBody CartSelectionRequest requestBody,
            HttpServletRequest request) {
        Long userId = requireUserId(request);
        return Result.success(cartService.updateSelected(userId, cartItemId, requestBody == null || requestBody.getSelected() == null || requestBody.getSelected()));
    }

    @PutMapping("/selected-all")
    public Result<Map<String, Object>> updateSelectedAll(
            @RequestBody CartSelectionRequest requestBody,
            HttpServletRequest request) {
        Long userId = requireUserId(request);
        return Result.success(cartService.updateSelectedAll(userId, requestBody == null || requestBody.getSelected() == null || requestBody.getSelected()));
    }

    @DeleteMapping("/{cartItemId}")
    public Result<Boolean> removeCartItem(@PathVariable Long cartItemId, HttpServletRequest request) {
        Long userId = requireUserId(request);
        return Result.success(cartService.removeCartItem(userId, cartItemId));
    }

    @DeleteMapping("/clear")
    public Result<Boolean> clearCart(HttpServletRequest request) {
        Long userId = requireUserId(request);
        return Result.success(cartService.clearCart(userId));
    }

    @GetMapping("/count")
    public Result<Long> getCartCount(HttpServletRequest request) {
        Long userId = requireUserId(request);
        return Result.success(cartService.getCartCount(userId));
    }

    @Data
    static class AddCartRequest {
        @Min(value = 1, message = "购买数量至少为 1")
        private Integer quantity = 1;
    }

    @Data
    static class UpdateCartQuantityRequest {
        @Min(value = 1, message = "购买数量至少为 1")
        private Integer quantity;
    }

    @Data
    static class CartSelectionRequest {
        private Boolean selected;
    }

    private Long requireUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        return userId;
    }
}
