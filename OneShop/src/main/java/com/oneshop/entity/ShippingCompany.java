package com.oneshop.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.ArrayList; 
import java.util.List;     

@Entity
@Table(name = "shipping_companies") 
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShippingCompany {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long shippingId;

    @Column(length = 150, nullable = false) 
    private String name;

    @Column(length = 20) 
    private String phone; 

    @Column(nullable = false)
    private Boolean isActive = true; 

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ShippingRule> rules = new ArrayList<>(); 
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_shipping_company_id") // Sửa lại tên cột để tránh trùng lặp
    private ShippingCompany shippingCompany;

}