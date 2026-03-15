// src/main/java/com/oneshop/repository/PromotionRepository.java
package com.oneshop.repository;

import org.springframework.data.domain.Page; // <<< THÊM IMPORT NÀY
import org.springframework.data.domain.Pageable; // <<< THÊM IMPORT NÀY
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.oneshop.entity.Promotion;

import java.time.LocalDate; 
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    
    // Tìm tất cả khuyến mãi của một Shop
    List<Promotion> findByShopId(Long shopId);

    Page<Promotion> findByEndDateAfter(LocalDate date, Pageable pageable);
    @Query("SELECT p FROM Promotion p WHERE p.startDate <= :date AND p.endDate >= :date")
    List<Promotion> findActivePromotions(@Param("date") LocalDate date);
    Optional<Promotion> findByDiscountCodeAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            String discountCode,
            LocalDate date1,
            LocalDate date2
        );
    Optional<Promotion> findByDiscountCode(String discountCode);
    @Override
    Optional<Promotion> findById(Long id);
}