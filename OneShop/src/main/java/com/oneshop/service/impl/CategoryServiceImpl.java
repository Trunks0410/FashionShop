package com.oneshop.service.impl;

import com.oneshop.entity.Category;
import com.oneshop.repository.CategoryRepository;
import com.oneshop.repository.ProductRepository;
import com.oneshop.service.CategoryService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityNotFoundException;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryServiceImpl implements CategoryService {

    private static final Logger logger = LoggerFactory.getLogger(CategoryServiceImpl.class);

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Category> findAll() {
        logger.debug("Fetching all categories");
        return categoryRepository.findAll(Sort.by("name"));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Category> findById(Long id) {
        logger.debug("Finding category by ID: {}", id);
        return categoryRepository.findById(id);
    }

    @Override
    @Transactional
    public Category saveCategory(Category category) {
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên danh mục không được để trống.");
        }
        String trimmedName = category.getName().trim();
        category.setName(trimmedName);
        logger.info("Attempting to save category: {}", trimmedName);

        Optional<Category> existingCategory = categoryRepository.findByNameIgnoreCase(trimmedName);
        
        if (existingCategory.isPresent() && (category.getId() == null || !existingCategory.get().getId().equals(category.getId()))) {
             logger.warn("Category name '{}' already exists.", trimmedName);
             throw new IllegalArgumentException("Tên danh mục '" + trimmedName + "' đã tồn tại.");
        }

        if (category.getParentCategory() != null && category.getParentCategory().getId() != null) {
            if (category.getId() != null && category.getId().equals(category.getParentCategory().getId())) {
                 throw new IllegalArgumentException("Không thể chọn chính nó làm danh mục cha.");
            }
            Category parent = categoryRepository.findById(category.getParentCategory().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Danh mục cha không tồn tại."));
            category.setParentCategory(parent);
        } else {
            category.setParentCategory(null);
        }

        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public Category save(Category category) {
        logger.info("Redirecting save() call to saveCategory() for category: {}", category.getName());
        return this.saveCategory(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        logger.warn("Attempting to delete category ID: {}", id);
        long productCount = productRepository.countByCategoryId(id);
        if (productCount > 0) {
            logger.warn("Cannot delete category {} used by {} products.", id, productCount);
            throw new RuntimeException("Không thể xóa danh mục đang được sử dụng bởi " + productCount + " sản phẩm.");
        }

        if (categoryRepository.existsById(id)) {
            categoryRepository.deleteById(id);
            logger.info("Category {} deleted successfully.", id);
        } else {
            logger.warn("Category {} not found for deletion.", id);
            throw new EntityNotFoundException("Không tìm thấy danh mục để xóa với ID: " + id);
        }
    }
    
    @Override
    @Transactional
    public void deleteById(Long id) {
        logger.warn("Attempting to delete category by ID: {}", id);
        long productCount = productRepository.countByCategoryId(id);
        if (productCount > 0) {
            throw new RuntimeException("Không thể xóa danh mục đang được sử dụng bởi sản phẩm.");
        }
        if (categoryRepository.existsById(id)) {
            categoryRepository.deleteById(id);
            logger.info("Category {} deleted successfully by deleteById.", id);
        } else {
             logger.warn("Category {} not found for deletion (deleteById).", id);
             throw new EntityNotFoundException("Không tìm thấy danh mục để xóa với ID: " + id);
        }
    }
}