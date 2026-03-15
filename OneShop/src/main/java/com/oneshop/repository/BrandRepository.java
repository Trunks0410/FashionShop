// src/main/java/com/oneshop/repository/BrandRepository.java
package com.oneshop.repository;

import com.oneshop.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional; // <<< THÊM IMPORT NÀY

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {

    // <<< THÊM PHƯƠNG THỨC NÀY >>>
    /**
     * Tìm thương hiệu theo tên (không phân biệt hoa thường).
     * @param name Tên thương hiệu cần tìm.
     * @return Optional<Brand>.
     */
    Optional<Brand> findByNameIgnoreCase(String name);
}