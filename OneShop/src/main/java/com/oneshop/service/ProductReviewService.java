package com.oneshop.service;

import com.oneshop.dto.ReviewDTO;
import com.oneshop.entity.ProductReview;
import com.oneshop.repository.ProductReviewRepository;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class ProductReviewService {

    @Autowired
    private ProductReviewRepository reviewRepository;

   
    
    @Transactional(readOnly = true)
    public List<ReviewDTO> getReviewsByProductId(Long productId) {
        return reviewRepository.findReviewDTOsByProductId(productId);
    }
}
