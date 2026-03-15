package com.oneshop.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCartRequest {
	private Long variantId; // Tên trường phải là "variantId"
    private int quantity;
}