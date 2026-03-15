package com.oneshop.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "product_variants") // Đã chuyển thành chữ thường để tránh lỗi case-sensitive của PostgreSQL
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long variantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Đã xóa bỏ columnDefinition = "nvarchar(100)", chỉ dùng length = 100
    @Column(length = 100, nullable = false) 
    private String name; 

    @Column(length = 50, unique = true)
    private String sku;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal price; 

    @Column(name = "original_price", precision = 12, scale = 2)
    private BigDecimal originalPrice; 

    @Column(nullable = false)
    private int stock; 

    @Column(name = "image_url")
    private String imageUrl; 

    @Column(name = "is_active", nullable = false)
    private boolean active = true; 
}