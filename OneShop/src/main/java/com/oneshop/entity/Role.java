// src/main/java/com/oneshop/entity/Role.java
package com.oneshop.entity; // Đảm bảo đúng package

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Entity
@Table(name = "ROLES")
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50) 
    @Enumerated(EnumType.STRING)
    private RoleName name;

    public enum RoleName {
        ADMIN,
        USER,
        VENDOR,
        SHIPPER
    }

    public Role(RoleName name) {
        this.name = name;
    }
}