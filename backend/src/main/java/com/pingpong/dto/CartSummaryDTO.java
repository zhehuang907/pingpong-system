package com.pingpong.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartSummaryDTO {
    private Integer itemCount;
    private Integer totalQuantity;
    private Integer selectedItemCount;
    private Integer selectedQuantity;
    private BigDecimal totalAmount;
    private BigDecimal selectedAmount;
    private Boolean allSelected;
}
