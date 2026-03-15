package com.oneshop.entity;

import com.oneshop.enums.ShopStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "shops") // Đã đổi "Shops" thành "shops" chữ thường
@Setter
public class Shop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Đã xóa nvarchar(255), dùng length = 255
    @Column(nullable = false, length = 255)
    private String name;

    // Đã xóa nvarchar(1000), giữ lại length = 1000
    @Column(length = 1000)
    private String description;

    private String logo;
    private String banner;
    private String contactEmail;
    private String contactPhone;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ShopStatus status = ShopStatus.PENDING;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @OneToMany(mappedBy = "shop", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Product> products;

    @OneToMany(mappedBy = "shop", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Order> orders;

    @OneToMany(mappedBy = "shop", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Promotion> promotions;
    
    // Đã xóa columnDefinition, giữ lại cấu hình precision và scale tiêu chuẩn
    @Column(name = "commission_rate", precision = 5, scale = 4, nullable = false)
    private BigDecimal commissionRate = new BigDecimal("0.05");

    @Column(name = "commission_updated_at")
    private LocalDateTime commissionUpdatedAt;
}