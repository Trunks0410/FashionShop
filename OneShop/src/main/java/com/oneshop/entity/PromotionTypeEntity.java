package com.oneshop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "promotion_types")
public class PromotionTypeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String code; 

    @Column(nullable = false, length = 255)
    private String name; 

    public PromotionTypeEntity(String code, String name) {
        this.code = code;
        this.name = name;
    }
}