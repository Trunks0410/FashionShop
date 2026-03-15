package com.oneshop.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.oneshop.entity.Shop;
import com.oneshop.enums.ShopStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Long> {

    // Tìm Shop dựa trên User (chủ sở hữu)
    Optional<Shop> findByUserId(Long userId);
    List<Shop> findByStatus(ShopStatus status, Sort sort);
}