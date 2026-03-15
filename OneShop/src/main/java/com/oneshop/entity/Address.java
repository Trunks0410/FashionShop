package com.oneshop.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "addresses") 
@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long addressId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private User user;

    @Column(name = "full_name", length = 100) 
    private String fullName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "is_default")
    private Boolean isDefault = false;

    @Column(name = "address", columnDefinition = "TEXT") 
    private String address; 
}