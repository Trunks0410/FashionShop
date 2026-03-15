// src/main/java/com/oneshop/repository/ProductReviewRepository.java
package com.oneshop.repository;

import com.oneshop.entity.ProductReview;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

import com.oneshop.dto.ReviewDTO;
import com.oneshop.entity.Order; // Import nếu cần findByOrderAndProduct
import com.oneshop.entity.Product; // Import nếu cần findByOrderAndProduct

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    // Hàm tìm review cụ thể trong đơn hàng (giữ lại)
    Optional<ProductReview> findByOrderAndProduct(Order order, Product product);

    // Tính rating trung bình (giữ lại)
    @Query("SELECT AVG(r.rating) FROM ProductReview r WHERE r.product.productId = :productId")
    Double findAverageRatingByProductId(@Param("productId") Long productId);

    // Đếm số lượng review (giữ lại)
    @Query("SELECT COUNT(r) FROM ProductReview r WHERE r.product.productId = :productId")
    Integer countReviewsByProductId(@Param("productId") Long productId);

   
    @Query("""
    	    SELECT new com.oneshop.dto.ReviewDTO(
    	        r.reviewId, r.comment, r.rating, r.reviewDate, u.username, rm.mediaUrl
    	    )
    	    FROM ProductReview r
    	    LEFT JOIN r.user u
    	    LEFT JOIN r.media rm
    	    WHERE r.product.productId = :productId
    	    ORDER BY r.reviewDate DESC
    	    """)
    	List<ReviewDTO> findReviewDTOsByProductId(@Param("productId") Long productId);
   
}