package com.oneshop.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDto {
    private Long productId;
    private String name;
    private BigDecimal price;
    private int quantity;
    private String imageUrl; // Để hiển thị ảnh trong trang giỏ hàng

    // Tính tổng tiền cho sản phẩm này
    public BigDecimal getSubtotal() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}