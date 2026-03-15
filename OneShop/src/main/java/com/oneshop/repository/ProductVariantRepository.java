package com.oneshop.repository;

import com.oneshop.entity.ProductVariant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
	ProductVariant findByVariantId(Long variantId);
	List<ProductVariant> findByProduct_ProductId(Long productId);
	// Tạo interface custom
	interface ProductVariantRepositoryCustom {
	    void refresh(ProductVariant variant);
	}

	// Implement interface custom
	@Repository
	class ProductVariantRepositoryCustomImpl implements ProductVariantRepositoryCustom {
	    
	    @PersistenceContext
	    private EntityManager entityManager;
	    
	    @Override
	    public void refresh(ProductVariant variant) {
	        entityManager.refresh(variant);
	    }
	}
}