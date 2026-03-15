package com.oneshop.service.impl;

import com.oneshop.entity.PromotionTypeEntity;
import com.oneshop.repository.PromotionTypeRepository;
import com.oneshop.service.PromotionTypeService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PromotionTypeServiceImpl implements PromotionTypeService {

    @Autowired
    private PromotionTypeRepository promotionTypeRepository;
    
    // (Inject PromotionRepository nếu muốn kiểm tra trước khi xóa)

    @Override
    public List<PromotionTypeEntity> findAll() {
        return promotionTypeRepository.findAll();
    }

    @Override
    @Transactional
    public PromotionTypeEntity savePromotionType(PromotionTypeEntity promotionType) {
        // (Thêm validation code/name không trùng nếu cần)
        return promotionTypeRepository.save(promotionType);
    }

    @Override
    @Transactional
    public void deletePromotionType(Long id) {
        // (Quan trọng: Thêm logic kiểm tra xem có Promotion nào đang dùng Type này không trước khi xóa)
        promotionTypeRepository.findById(id).ifPresentOrElse(
            type -> promotionTypeRepository.delete(type),
            () -> { throw new RuntimeException("Không tìm thấy loại KM để xóa với ID: " + id); }
        );
    }
}