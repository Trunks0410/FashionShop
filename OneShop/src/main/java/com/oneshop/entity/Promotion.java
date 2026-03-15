package com.oneshop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "promotions") 
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String campaignName; 

    @Column(nullable = false, unique = true, length = 100)
    private String discountCode; 

    @ManyToOne(fetch = FetchType.EAGER) 
    @JoinColumn(name = "promotion_type_id", nullable = false)
    private PromotionTypeEntity type;

    // numeric(19,2) là cú pháp SQL Server, JPA tự xử lý thông qua precision và scale
    @Column(precision = 19, scale = 2)
    private BigDecimal value;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;
}