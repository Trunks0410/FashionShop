package com.oneshop.service;

import java.util.List;

import com.oneshop.entity.PromotionTypeEntity;

public interface PromotionTypeService {
    List<PromotionTypeEntity> findAll();
    PromotionTypeEntity savePromotionType(PromotionTypeEntity promotionType);
    void deletePromotionType(Long id);
}