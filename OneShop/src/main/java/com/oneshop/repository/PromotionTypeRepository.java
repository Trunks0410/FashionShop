package com.oneshop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.oneshop.entity.PromotionTypeEntity;


@Repository
public interface PromotionTypeRepository extends JpaRepository<PromotionTypeEntity, Long> {
}