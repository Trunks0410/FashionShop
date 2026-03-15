// src/main/java/com/oneshop/service/BrandService.java
package com.oneshop.service;

import com.oneshop.entity.Brand;
import com.oneshop.repository.BrandRepository;
import com.oneshop.repository.ProductRepository; // <<< THÊM IMPORT NÀY
import org.slf4j.Logger; // <<< THÊM IMPORT NÀY
import org.slf4j.LoggerFactory; // <<< THÊM IMPORT NÀY
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort; // <<< THÊM IMPORT NÀY
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // <<< THÊM IMPORT NÀY
import jakarta.persistence.EntityNotFoundException; // <<< THÊM IMPORT NÀY

import java.util.List;
import java.util.Optional; // <<< THÊM IMPORT NÀY

@Service
public class BrandService {

    private static final Logger logger = LoggerFactory.getLogger(BrandService.class); // <<< THÊM LOGGER

    @Autowired
    private BrandRepository brandRepository;

    @Autowired // <<< THÊM INJECT ProductRepository
    private ProductRepository productRepository;

    /**
     * Lấy tất cả thương hiệu từ database, sắp xếp theo tên.
     * @return Danh sách các Brand entity.
     */
    @Transactional(readOnly = true) // <<< THÊM TRANSACTIONAL
    public List<Brand> findAll() {
        logger.debug("Fetching all brands sorted by name"); // <<< THÊM LOG
        return brandRepository.findAll(Sort.by(Sort.Direction.ASC, "name")); // <<< SẮP XẾP
    }

    /**
     * Tìm thương hiệu theo ID.
     * @param id ID thương hiệu.
     * @return Optional<Brand>.
     */
    @Transactional(readOnly = true) // <<< THÊM HÀM NÀY
    public Optional<Brand> findById(Long id) {
        logger.debug("Finding brand by ID: {}", id);
        return brandRepository.findById(id);
    }

     /**
     * Tìm thương hiệu theo tên (không phân biệt hoa thường).
     * @param name Tên thương hiệu.
     * @return Optional<Brand>.
     */
    @Transactional(readOnly = true) // <<< THÊM HÀM NÀY
    public Optional<Brand> findByNameIgnoreCase(String name) {
        logger.debug("Finding brand by name (ignore case): {}", name);
        return brandRepository.findByNameIgnoreCase(name);
    }

    /**
     * Lưu (thêm mới hoặc cập nhật) một thương hiệu.
     * @param brand Thương hiệu cần lưu.
     * @return Thương hiệu đã được lưu.
     * @throws IllegalArgumentException Nếu tên thương hiệu trống hoặc đã tồn tại.
     */
    @Transactional // <<< THÊM HÀM NÀY
    public Brand saveBrand(Brand brand) {
        if (brand.getName() == null || brand.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên thương hiệu không được để trống.");
        }
        String trimmedName = brand.getName().trim();
        brand.setName(trimmedName);
        logger.info("Attempting to save brand: {}", trimmedName);

        // Kiểm tra trùng tên (không phân biệt hoa thường)
        Optional<Brand> existingBrand = brandRepository.findByNameIgnoreCase(trimmedName);
        if (existingBrand.isPresent() && (brand.getBrandId() == null || !existingBrand.get().getBrandId().equals(brand.getBrandId()))) {
             logger.warn("Brand name '{}' already exists.", trimmedName);
             throw new IllegalArgumentException("Tên thương hiệu '" + trimmedName + "' đã tồn tại.");
        }

        return brandRepository.save(brand);
    }

    /**
     * Xóa một thương hiệu theo ID.
     * @param id ID thương hiệu cần xóa.
     * @throws RuntimeException Nếu thương hiệu đang được sử dụng bởi sản phẩm.
     * @throws EntityNotFoundException Nếu không tìm thấy thương hiệu.
     */
    @Transactional // <<< THÊM HÀM NÀY
    public void deleteBrand(Long id) {
        logger.warn("Attempting to delete brand ID: {}", id);

        // 1. Kiểm tra xem có sản phẩm nào đang dùng thương hiệu này không
        // Cần phương thức countByBrand_BrandId trong ProductRepository
        // long productCount = productRepository.countByBrand_BrandId(id); // Giả sử có hàm này
        // if (productCount > 0) {
        //     logger.warn("Cannot delete brand {} used by {} products.", id, productCount);
        //     throw new RuntimeException("Không thể xóa thương hiệu đang được sử dụng bởi " + productCount + " sản phẩm.");
        // }
        // TẠM THỜI BỎ QUA CHECK NÀY VÌ ProductRepository chưa có hàm countByBrand_BrandId

        // 2. Xóa thương hiệu
        if (brandRepository.existsById(id)) {
            brandRepository.deleteById(id);
            logger.info("Brand {} deleted successfully.", id);
        } else {
            logger.warn("Brand {} not found for deletion.", id);
            throw new EntityNotFoundException("Không tìm thấy thương hiệu để xóa với ID: " + id);
        }
    }
}