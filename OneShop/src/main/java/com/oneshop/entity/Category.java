package com.oneshop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Đã thay thế columnDefinition = "nvarchar(255)" bằng length = 255
    @Column(nullable = false, length = 255) 
    private String name;

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private List<Product> products;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id") 
    private Category parentCategory;
}