package com.oneshop.service;

import com.oneshop.dto.CartDto;
import com.oneshop.dto.PromotionDto;
import com.oneshop.entity.Promotion;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional; // Thêm Optional

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PromotionService {

    /** Lấy tất cả khuyến mãi của một shop */
    List<Promotion> getPromotionsByShop(Long shopId);

    /** Tạo một khuyến mãi mới */
    Promotion createPromotion(PromotionDto promotionDto, Long shopId);

    void deletePromotion(Long promotionId, Long shopId);
    
    Optional<Promotion> findPromotionById(Long promotionId);

    /** Lấy danh sách các khuyến mãi còn hạn (phân trang). */
    Page<Promotion> findActiveAndUpcomingPromotions(Pageable pageable);

    // --- THÊM CÁC PHƯƠNG THỨC MỚI ---
    /**
     * Tìm khuyến mãi theo ID và kiểm tra quyền sở hữu của shop.
     * @param promotionId ID khuyến mãi.
     * @param shopId ID của shop.
     * @return Optional<Promotion>.
     */
    Optional<Promotion> getPromotionByIdAndShopId(Long promotionId, Long shopId);

    /**
     * Cập nhật thông tin khuyến mãi.
     * @param promotionId ID khuyến mãi cần cập nhật.
     * @param promotionDto Dữ liệu mới.
     * @param shopId ID của shop (để kiểm tra quyền).
     * @return Promotion đã được cập nhật.
     */
    Promotion updatePromotion(Long promotionId, PromotionDto promotionDto, Long shopId);
    // --- KẾT THÚC PHẦN THÊM ---
    List<Promotion> findApplicablePromotions(CartDto cart, String username);
    CartDto applyVoucher(String username, String voucherCode) throws IllegalArgumentException;
    CartDto removeVoucher(String username);
    public BigDecimal calculateDiscountForOrder(CartDto cart, BigDecimal subtotal);
    public Optional<Promotion> findByDiscountCode(String code);
}