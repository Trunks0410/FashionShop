package com.oneshop.repository;

import com.oneshop.entity.CartItem;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
	Optional<CartItem> findByCart_CartIdAndVariant_VariantId(Long cartId, Long variantId);
	@Transactional // <-- THÊM DÒNG NÀY
	@Modifying   // <-- THÊM DÒNG NÀY
	@Query("DELETE FROM CartItem ci WHERE ci.cart.user.id = :userId AND ci.variant.id IN :variantIds")
	void deleteByUserIdAndProductVariantIdIn(@Param("userId") Long userId, @Param("variantIds") List<Long> variantIds);
}