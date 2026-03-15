package com.oneshop.service;

import com.oneshop.dto.CartDto;
import com.oneshop.dto.CartItemDto;
import com.oneshop.entity.Cart;
import com.oneshop.entity.CartItem;
import com.oneshop.entity.ProductVariant;
import com.oneshop.entity.User;
import com.oneshop.repository.CartItemRepository;
import com.oneshop.repository.CartRepository;
import com.oneshop.service.impl.PromotionServiceImpl;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger; 
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CartService {

    private static final Logger logger = LoggerFactory.getLogger(CartService.class);

    @Autowired private CartRepository cartRepository;
    @Autowired private CartItemRepository cartItemRepository;
    @Autowired private UserService userService;
    @Autowired private ProductVariantService productVariantService;

    private HttpSession getSession() {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attr == null) {
            return null; 
        }
        HttpServletRequest request = attr.getRequest();
        return request.getSession(false); 
    }

    // New Validation Method as requested by SSD & Use Case
    public void validateCartItemQuantity(int quantity, ProductVariant variant) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Số lượng không hợp lệ.");
        }
        if (variant != null && variant.getStock() < quantity) {
            throw new RuntimeException("Sản phẩm '" + variant.getProduct().getName() + " - " + variant.getName() + "' không đủ hàng (còn " + variant.getStock() + ").");
        }
    }

    @Transactional
    public CartDto getCartItems(String username) {
        User user = userService.findByUsername(username);
        Cart cart = findOrCreateCartByUser(user);
        return mapToCartDto(cart); 
    }

    @Transactional
    public CartDto addItemToCart(String username, Long variantId, int quantity) {
        User user = userService.findByUsername(username);
        Cart cart = findOrCreateCartByUser(user);
        
        ProductVariant variant = productVariantService.findOptionalVariantById(variantId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể sản phẩm: " + variantId));

        if (quantity <= 0) quantity = 1;

        Optional<CartItem> existingItemOpt = cartItemRepository.findByCart_CartIdAndVariant_VariantId(cart.getCartId(), variantId);
        int quantityInCart = existingItemOpt.map(CartItem::getQuantity).orElse(0);
        int requestedTotalQuantity = quantityInCart + quantity;

        validateCartItemQuantity(requestedTotalQuantity, variant); // Applying validation

        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            existingItem.setQuantity(requestedTotalQuantity);
            cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setVariant(variant);
            newItem.setQuantity(quantity); 
            cart.getItems().add(newItem); 
            cartItemRepository.save(newItem);
        }

        Cart updatedCart = cartRepository.findByIdWithItems(cart.getCartId()).orElse(cart);
        return mapToCartDto(updatedCart);
    }

    @Transactional
    public CartDto updateCartItemQuantity(String username, Long variantId, int newQuantity) {
        User user = userService.findByUsername(username);
        Cart cart = findOrCreateCartByUser(user);

        CartItem cartItem = cartItemRepository.findByCart_CartIdAndVariant_VariantId(cart.getCartId(), variantId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong giỏ hàng"));

        if (newQuantity <= 0) {
            cart.getItems().remove(cartItem); 
            cartItemRepository.delete(cartItem); 
        } else {
             ProductVariant variant = cartItem.getVariant();
             validateCartItemQuantity(newQuantity, variant); // Applying validation
             
            cartItem.setQuantity(newQuantity);
            cartItemRepository.save(cartItem);
        }

        Cart updatedCart = cartRepository.findByIdWithItems(cart.getCartId()).orElse(cart);
        return mapToCartDto(updatedCart);
    }

    @Transactional
    public void removeCartItem(String username, Long variantId) {
        User user = userService.findByUsername(username);
        Cart cart = cartRepository.findByUserIdWithItems(user.getId())
                 .orElseThrow(() -> new RuntimeException("Không tìm thấy giỏ hàng cho user: " + username));

        CartItem cartItemToRemove = cart.getItems().stream()
            .filter(item -> item.getVariant() != null && item.getVariant().getVariantId().equals(variantId))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong giỏ hàng"));

        try {
            cart.getItems().remove(cartItemToRemove);
            cartItemRepository.delete(cartItemToRemove);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi xóa sản phẩm khỏi giỏ hàng.", e);
        }
    }

    @Transactional
    public Cart findOrCreateCartByUser(User user) {
        return cartRepository.findByUserIdWithItems(user.getId())
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    Cart savedCart = cartRepository.save(newCart);
                    user.setCart(savedCart);
                    return savedCart;
                });
    }

    private CartDto mapToCartDto(Cart cart) {
        CartDto cartDto = new CartDto();
        Map<Long, CartItemDto> itemsMap = new HashMap<>();

        if (cart != null && cart.getItems() != null) {
            for (CartItem itemEntity : cart.getItems()) {
                ProductVariant variant = itemEntity.getVariant();
                if (variant == null || variant.getProduct() == null) continue;

                CartItemDto itemDto = new CartItemDto();
                itemDto.setProductId(variant.getVariantId());

                String productName = variant.getProduct().getName();
                String variantName = variant.getName();
                itemDto.setName(StringUtils.hasText(variantName) ? (productName + " - " + variantName) : productName);

                itemDto.setPrice(variant.getPrice());
                itemDto.setQuantity(itemEntity.getQuantity());

                String imageUrl = variant.getImageUrl();
                if (!StringUtils.hasText(imageUrl)) {
                     imageUrl = variant.getProduct().getPrimaryImageUrl();
                } else {
                     if (!imageUrl.startsWith("/")) {
                          imageUrl = "/uploads/images/" + imageUrl; 
                     }
                }
                itemDto.setImageUrl(imageUrl);
                itemsMap.put(itemDto.getProductId(), itemDto);
            }
        }

        cartDto.setItems(itemsMap);
        
        HttpSession session = getSession();
        if (session != null) {
            cartDto.setAppliedVoucherCode((String) session.getAttribute(PromotionServiceImpl.VOUCHER_CODE_SESSION_KEY));
            cartDto.setDiscountAmount((BigDecimal) session.getAttribute(PromotionServiceImpl.VOUCHER_DISCOUNT_SESSION_KEY));
            cartDto.setAppliedVoucherTypeCode((String) session.getAttribute(PromotionServiceImpl.VOUCHER_TYPE_CODE_SESSION_KEY));
            cartDto.setAppliedVoucherValue((BigDecimal) session.getAttribute(PromotionServiceImpl.VOUCHER_VALUE_SESSION_KEY));
        }
        
        cartDto.calculateTotals();
        return cartDto;
    }

    @Transactional
    public void clearCartItems(Long userId, List<Long> variantIds) {
         if (userId == null || variantIds == null || variantIds.isEmpty()) return;
         cartItemRepository.deleteByUserIdAndProductVariantIdIn(userId, variantIds);
    }
}