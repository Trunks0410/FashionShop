package com.oneshop.repository;

import com.oneshop.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional; // Import Optional

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // <<< BỔ SUNG >>>
    Optional<Category> findByNameIgnoreCase(String name);
}