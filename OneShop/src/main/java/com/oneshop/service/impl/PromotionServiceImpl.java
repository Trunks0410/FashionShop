package com.oneshop.service.impl;

import com.oneshop.dto.CartDto;
import com.oneshop.dto.PromotionDto;
import com.oneshop.entity.Promotion;
import com.oneshop.entity.PromotionTypeEntity;
import com.oneshop.entity.Shop;
import com.oneshop.repository.OrderRepository; 
import com.oneshop.repository.PromotionRepository;
import com.oneshop.repository.PromotionTypeRepository;
import com.oneshop.repository.ShopRepository;
import com.oneshop.service.CartService;
import com.oneshop.service.PromotionService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PromotionServiceImpl implements PromotionService {

    private static final Logger logger = LoggerFactory.getLogger(PromotionServiceImpl.class);

    // Session keys
    public static final String VOUCHER_CODE_SESSION_KEY = "appliedVoucherCode";
    public static final String VOUCHER_DISCOUNT_SESSION_KEY = "appliedDiscountAmount";
    public static final String VOUCHER_TYPE_CODE_SESSION_KEY = "appliedVoucherTypeCode";
    public static final String VOUCHER_VALUE_SESSION_KEY = "appliedVoucherValue";

    @Autowired private PromotionRepository promotionRepository;
    @Autowired private ShopRepository shopRepository;
    @Autowired private PromotionTypeRepository promotionTypeRepository;
    @Autowired private CartService cartService;
    
    @Autowired private OrderRepository orderRepository; 

    @Override
    @Transactional(readOnly = true)
    public List<Promotion> getPromotionsByShop(Long shopId) {
        logger.debug("Fetching promotions for shopId: {}", shopId);
        return promotionRepository.findByShopId(shopId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Promotion createPromotion(PromotionDto promotionDto, Long shopId) {
        logger.info("Creating new promotion for shopId: {}", shopId);
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy shop với ID: " + shopId));

        PromotionTypeEntity promotionType = promotionTypeRepository.findById(promotionDto.getPromotionTypeId())
                .orElseThrow(() -> new IllegalArgumentException("Loại khuyến mãi không hợp lệ."));
        
        Promotion promotion = new Promotion();
        mapDtoToEntity(promotionDto, promotion, promotionType, shop);

        Promotion savedPromotion = promotionRepository.save(promotion);
        logger.info("Promotion created successfully with ID: {}", savedPromotion.getId());
        return savedPromotion;
    }

    private HttpSession getSession() {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        if (attr == null) {
            throw new RuntimeException("Không thể truy cập RequestContext. Service này có đang được gọi từ một HTTP request không?");
        }
        HttpServletRequest request = attr.getRequest();
        return request.getSession(true);
    }

    @Override
    @Transactional(readOnly = true)
    public CartDto applyVoucher(String username, String voucherCode) throws IllegalArgumentException {
        CartDto cart = cartService.getCartItems(username); // Đồng bộ method
        if (cart == null || cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng trống, không thể áp dụng voucher.");
        }

        LocalDate today = LocalDate.now();
        Promotion promo = promotionRepository.findByDiscountCodeAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                voucherCode, today, today)
                .orElseThrow(() -> new IllegalArgumentException("Mã voucher không hợp lệ hoặc đã hết hạn."));

        if (!isPromotionApplicable(promo, cart, username)) {
             throw new IllegalArgumentException("Voucher không đủ điều kiện áp dụng cho giỏ hàng này.");
        }

        BigDecimal discountAmount = calculateDiscount(promo, cart.getSubtotal());

        HttpSession session = getSession();
        session.setAttribute(VOUCHER_CODE_SESSION_KEY, promo.getDiscountCode());
        session.setAttribute(VOUCHER_DISCOUNT_SESSION_KEY, discountAmount);
        session.setAttribute(VOUCHER_TYPE_CODE_SESSION_KEY, promo.getType().getCode());
        session.setAttribute(VOUCHER_VALUE_SESSION_KEY, promo.getValue());

        cart.setAppliedVoucherCode(promo.getDiscountCode());
        cart.setDiscountAmount(discountAmount);
        cart.setAppliedVoucherTypeCode(promo.getType().getCode());
        cart.setAppliedVoucherValue(promo.getValue());
        cart.calculateTotals();

        return cart;
    }

    @Override
    @Transactional(readOnly = true)
    public CartDto removeVoucher(String username) {
        HttpSession session = getSession();
        
        session.removeAttribute(VOUCHER_CODE_SESSION_KEY);
        session.removeAttribute(VOUCHER_DISCOUNT_SESSION_KEY);
        session.removeAttribute(VOUCHER_TYPE_CODE_SESSION_KEY);
        session.removeAttribute(VOUCHER_VALUE_SESSION_KEY);
        
        return cartService.getCartItems(username); // Đồng bộ method
    }

    @Override
    public List<Promotion> findApplicablePromotions(CartDto cart, String username) {
        LocalDate today = LocalDate.now();
        List<Promotion> activePromotions = promotionRepository.findActivePromotions(today);

        return activePromotions.stream()
             .filter(promo -> isPromotionApplicable(promo, cart, username))
             .collect(Collectors.toList());
    }

    private boolean isPromotionApplicable(Promotion promo, CartDto cart, String username) {
        if (promo == null || cart == null || cart.getSubtotal() == null) {
            return false;
        }
        return true; 
    }

    private BigDecimal calculateDiscount(Promotion promo, BigDecimal subtotal) {
        if (promo == null || promo.getType() == null) {
            return BigDecimal.ZERO;
        }

        String typeCode = promo.getType().getCode();
        BigDecimal value = promo.getValue();
        
        if (value == null) {
            if ("FREE_SHIPPING".equalsIgnoreCase(typeCode)) {
                return BigDecimal.ZERO;
            }
            return BigDecimal.ZERO;
        }

        if ("PERCENTAGE".equalsIgnoreCase(typeCode)) {
            BigDecimal discount = subtotal.multiply(value).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            return discount;
        } else if ("FIXED_AMOUNT".equalsIgnoreCase(typeCode)) {
            return value.min(subtotal);
        } else if ("FREE_SHIPPING".equalsIgnoreCase(typeCode)) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.ZERO;
    }

    @Override
    @Transactional
    public void deletePromotion(Long promotionId, Long shopId) {
        logger.warn("Attempting to delete promotion ID: {} for shop ID: {}", promotionId, shopId);
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khuyến mãi với ID: " + promotionId));

        if (promotion.getShop() == null || !promotion.getShop().getId().equals(shopId)) {
            logger.warn("Security violation: Shop {} tried to delete promotion {} of shop {}",
                        shopId, promotionId, promotion.getShop() != null ? promotion.getShop().getId() : "null");
            throw new SecurityException("Bạn không có quyền xóa khuyến mãi này");
        }

        long orderUsageCount = orderRepository.countByPromotionId(promotionId);
        
        if (orderUsageCount > 0) {
            logger.warn("Cannot delete promotion ID {}: It is used by {} order(s).", promotionId, orderUsageCount);
            throw new RuntimeException("Không thể xóa khuyến mãi này. Nó đã được sử dụng trong " + orderUsageCount + " đơn hàng.");
        }

        promotionRepository.delete(promotion);
        logger.info("Promotion ID: {} deleted successfully.", promotionId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Promotion> findActiveAndUpcomingPromotions(Pageable pageable) {
        logger.debug("Fetching active and upcoming promotions with pageable: {}", pageable);
        return promotionRepository.findByEndDateAfter(LocalDate.now().minusDays(1), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Promotion> getPromotionByIdAndShopId(Long promotionId, Long shopId) {
        logger.debug("Fetching promotion ID: {} for shop ID: {}", promotionId, shopId);
        return promotionRepository.findById(promotionId)
                .filter(promo -> promo.getShop() != null && promo.getShop().getId().equals(shopId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Promotion updatePromotion(Long promotionId, PromotionDto promotionDto, Long shopId) {
        logger.info("Updating promotion ID: {} for shop ID: {}", promotionId, shopId);
        Promotion existingPromotion = getPromotionByIdAndShopId(promotionId, shopId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khuyến mãi hoặc bạn không có quyền sửa."));

        PromotionTypeEntity promotionType = promotionTypeRepository.findById(promotionDto.getPromotionTypeId())
                .orElseThrow(() -> new IllegalArgumentException("Loại khuyến mãi không hợp lệ."));

        mapDtoToEntity(promotionDto, existingPromotion, promotionType, existingPromotion.getShop());

        Promotion updatedPromotion = promotionRepository.save(existingPromotion);
        logger.info("Promotion ID: {} updated successfully.", updatedPromotion.getId());
        return updatedPromotion;
    }

    private void mapDtoToEntity(PromotionDto dto, Promotion entity, PromotionTypeEntity type, Shop shop) {
        entity.setCampaignName(dto.getCampaignName().trim());
        entity.setDiscountCode(dto.getDiscountCode().trim());
        entity.setType(type);
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setShop(shop);

        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new IllegalArgumentException("Ngày kết thúc không được trước ngày bắt đầu.");
        }

        if (!"FREE_SHIPPING".equals(type.getCode())) {
            if (dto.getDiscountValue() == null || dto.getDiscountValue().compareTo(BigDecimal.ZERO) < 0) {
                 throw new IllegalArgumentException("Giá trị giảm không hợp lệ cho loại khuyến mãi '" + type.getName() + "'.");
            }
            if ("PERCENTAGE".equalsIgnoreCase(type.getCode()) && dto.getDiscountValue().compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("Giá trị giảm theo % không được lớn hơn 100.");
            }
             entity.setValue(dto.getDiscountValue());
        } else {
            entity.setValue(null);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateDiscountForOrder(CartDto cart, BigDecimal subtotal) {
        if (cart.getAppliedVoucherCode() == null || subtotal == null || subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        String typeCode = cart.getAppliedVoucherTypeCode();
        BigDecimal value = cart.getAppliedVoucherValue();
        
        if (value == null) {
            return BigDecimal.ZERO;
        }
        
        if ("PERCENTAGE".equalsIgnoreCase(typeCode)) {
            BigDecimal discount = subtotal.multiply(value)
                                    .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            return discount.setScale(0, RoundingMode.HALF_UP).max(BigDecimal.ZERO);

        } else if ("FIXED_AMOUNT".equalsIgnoreCase(typeCode)) {
            return value.min(subtotal).setScale(0, RoundingMode.HALF_UP).max(BigDecimal.ZERO);

        } else if ("FREE_SHIPPING".equalsIgnoreCase(typeCode)) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.ZERO;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Promotion> findByDiscountCode(String code) {
        return promotionRepository.findByDiscountCode(code);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Promotion> findPromotionById(Long promotionId) {
        return promotionRepository.findById(promotionId);
    }
    
}