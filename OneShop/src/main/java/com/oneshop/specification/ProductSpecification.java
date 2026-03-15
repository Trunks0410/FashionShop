// src/main/java/com/oneshop/specification/ProductSpecification.java
package com.oneshop.specification;

import com.oneshop.entity.Product;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;

import com.oneshop.enums.ProductStatus;

public class ProductSpecification {

    // --- CÁC HÀM CŨ GIỮ NGUYÊN ---
    public static Specification<Product> hasCategory(List<Long> categoryIds) {
        return (root, query, criteriaBuilder) -> {
            if (categoryIds == null || categoryIds.isEmpty()) {
                return criteriaBuilder.conjunction(); // Không lọc nếu list rỗng
            }
            // Giả sử Category entity có trường 'id'
            return root.get("category").get("id").in(categoryIds);
        };
    }

    public static Specification<Product> hasBrand(List<Long> brandIds) {
        return (root, query, criteriaBuilder) -> {
            if (brandIds == null || brandIds.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
             // Giả sử Brand entity có trường 'brandId'
            return root.get("brand").get("brandId").in(brandIds);
        };
    }

    public static Specification<Product> priceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            // Trả về Predicate kết hợp hoặc Predicate luôn đúng nếu không có điều kiện giá
            if (predicates.isEmpty()) {
                 return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Product> hasName(String name) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(name)) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%" + name.toLowerCase() + "%");
        };
    }

    public static Specification<Product> hasCategoryId(Long categoryId) {
        return (root, query, criteriaBuilder) -> {
            if (categoryId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("category").get("id"), categoryId);
        };
    }

    public static Specification<Product> hasBrandId(Long brandId) {
        return (root, query, criteriaBuilder) -> {
            if (brandId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("brand").get("brandId"), brandId);
        };
    }

    // --- >>> THÊM PHƯƠNG THỨC NÀY <<< ---
    /**
     * Tạo Specification để lọc các sản phẩm đã được published (published = true).
     * @return Specification.
     */
    public static Specification<Product> isPublished() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.isTrue(root.get("published")); // Giả sử trường boolean tên là "published"
    }
    // --- >>> KẾT THÚC PHƯƠNG THỨC MỚI <<< ---
    public static Specification<Product> isSelling() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("status"), ProductStatus.SELLING); // Lọc theo status = SELLING
    }
    /**
     * Lọc sản phẩm có điểm đánh giá (rating) lớn hơn hoặc bằng giá trị truyền vào
     */
    public static Specification<Product> hasRatingGreaterThanOrEqual(Integer rating) {
        return (root, query, criteriaBuilder) -> {
            if (rating == null) {
                return criteriaBuilder.conjunction();
            }
            // Giả sử trường 'rating' trong entity Product có kiểu dữ liệu là Double hoặc Float
            return criteriaBuilder.greaterThanOrEqualTo(root.get("rating"), rating.doubleValue());
        };
    }
}