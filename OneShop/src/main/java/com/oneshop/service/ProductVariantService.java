package com.oneshop.service;

import com.oneshop.entity.ProductVariant;
import com.oneshop.repository.ProductVariantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional; // Import Optional

@Service
public class ProductVariantService {
    private static final Logger logger = LoggerFactory.getLogger(ProductVariantService.class);
    @Autowired private ProductVariantRepository productVariantRepository;

    /** @deprecated Nên dùng findOptionalVariantById */
    @Transactional(readOnly = true)
    public ProductVariant findVariantById(Long variantId) {
         logger.debug("Finding product variant by ID: {}", variantId);
         // Giả sử repo có hàm findByVariantId trả về ProductVariant hoặc null
         // return productVariantRepository.findByVariantId(variantId);
         // Nên dùng findById
         return productVariantRepository.findById(variantId).orElse(null);
    }

    @Transactional(readOnly = true)
    public Optional<ProductVariant> findOptionalVariantById(Long variantId) { // <<< THÊM HÀM NÀY
        logger.debug("Finding optional product variant by ID: {}", variantId);
        return productVariantRepository.findById(variantId);
    }
}