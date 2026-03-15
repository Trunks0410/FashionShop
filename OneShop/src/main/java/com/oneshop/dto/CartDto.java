package com.oneshop.dto;

// KHÔNG IMPORT PromotionType ENUM
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects; 

@Data
@NoArgsConstructor
public class CartDto {

    private Map<Long, CartItemDto> items = new HashMap<>();
    private BigDecimal subtotal; 

    private String appliedVoucherCode; 
    private BigDecimal discountAmount; 
    
    // === TRƯỜNG MỚI ĐỂ ĐỒNG BỘ CLIENT/SERVER ===
    /** Code của loại voucher (vd: PERCENT, FIXED, FREESHIP) */
    private String appliedVoucherTypeCode; // <-- THAY PromotionType ENUM

    /** Giá trị gốc của voucher (vd: 10, 50000) */
    private BigDecimal appliedVoucherValue; // <-- GIỮ NGUYÊN
    // ===========================================

    private BigDecimal grandTotal;

    public void calculateTotals() {
        // ... (Logic tính subtotal không đổi)
        this.subtotal = items.values().stream()
                .map(CartItemDto::getSubtotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ... (Logic tính grandTotal không đổi)
        this.grandTotal = this.subtotal;
        if (this.discountAmount != null && this.discountAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.grandTotal = this.grandTotal.subtract(this.discountAmount);
        }
        if (this.grandTotal.compareTo(BigDecimal.ZERO) < 0) {
            this.grandTotal = BigDecimal.ZERO;
        }
    }

    public int getTotalItems() {
        return items.values().stream()
                .mapToInt(CartItemDto::getQuantity)
                .sum();
    }
}