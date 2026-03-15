package com.oneshop.controller;

import com.oneshop.dto.ApplyVoucherRequest;
import com.oneshop.dto.CartDto;
import com.oneshop.dto.UpdateCartRequest;
import com.oneshop.entity.Promotion;
import com.oneshop.service.CartService;
import com.oneshop.service.PromotionService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
public class CartController {

    private static final Logger logger = LoggerFactory.getLogger(CartController.class);

    @Autowired
    private CartService cartService;
    
    @Autowired
    private PromotionService promotion;

    @GetMapping("/cart")
    public String getCartItems(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
             logger.debug("User not authenticated, redirecting to login.");
             return "redirect:/login";
        }
        String username = authentication.getName();
        logger.debug("Viewing cart for user: {}", username);
        CartDto cart = cartService.getCartItems(username);

        List<Promotion> promos = promotion.findApplicablePromotions(cart, username);
        model.addAttribute("applicableVouchers", promos);
        model.addAttribute("cart", cart);
        return "user/cart"; 
    }

    @GetMapping("/cart/add/{variantId}")
    public String addToCartRedirect(@PathVariable("variantId") Long variantId,
                            @RequestParam(name = "quantity", defaultValue = "1") int quantity,
                            Authentication authentication, RedirectAttributes redirectAttributes) {
         if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
             return "redirect:/login";
         }
        String username = authentication.getName();
        try {
            cartService.addItemToCart(username, variantId, quantity);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm sản phẩm vào giỏ hàng!");
        } catch (RuntimeException e) {
             redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/cart";
    }

    @GetMapping("/cart/remove/{variantId}")
    public String removeCartItem(@PathVariable("variantId") Long variantId,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
             return "redirect:/login";
        }
        String username = authentication.getName();
        try {
            cartService.removeCartItem(username, variantId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa sản phẩm khỏi giỏ hàng.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa sản phẩm: " + e.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/api/cart/update")
    @ResponseBody
    public ResponseEntity<?> updateCartItemQuantity(
            @RequestBody UpdateCartRequest request,
            Authentication authentication
    ) {
         if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                  .body(Map.of("message", "Vui lòng đăng nhập."));
         }
        String username = authentication.getName();
        try {
            CartDto updatedCart = cartService.updateCartItemQuantity(
                username,
                request.getVariantId(),
                request.getQuantity()
            );
            return ResponseEntity.ok(updatedCart);
        } catch (RuntimeException e) {
             return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/api/cart/add/{variantId}")
    @ResponseBody
    public ResponseEntity<?> addToCartApi(
            @PathVariable("variantId") Long variantId,
            @RequestBody AddItemRequest request,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                 .body(Map.of("message", "Vui lòng đăng nhập để thêm vào giỏ hàng."));
        }
        String username = authentication.getName();
        int quantity = (request != null && request.getQuantity() > 0) ? request.getQuantity() : 1;
        try {
            CartDto updatedCart = cartService.addItemToCart(username, variantId, quantity);
            return ResponseEntity.ok(Map.of(
                "message", "Thêm vào giỏ thành công!",
                "totalItems", updatedCart.getTotalItems(),
                "cart", updatedCart 
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                  .body(Map.of("message", "Lỗi hệ thống khi thêm vào giỏ hàng."));
        }
    }

    @PostMapping("/api/cart/remove/{variantId}")
    @ResponseBody
    public ResponseEntity<?> removeCartItemApi(
            @PathVariable("variantId") Long variantId,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                 .body(Map.of("message", "Vui lòng đăng nhập."));
        }
        String username = authentication.getName();
        try {
            cartService.removeCartItem(username, variantId);
            CartDto updatedCart = cartService.getCartItems(username);
            return ResponseEntity.ok(Map.of(
                "message", "Đã xóa sản phẩm khỏi giỏ hàng.",
                "totalItems", updatedCart.getTotalItems(),
                "cart", updatedCart
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi khi xóa sản phẩm: " + e.getMessage()));
        } catch (Exception e) {
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                  .body(Map.of("message", "Lỗi hệ thống khi xóa sản phẩm."));
        }
    }
    
    @PostMapping("/api/cart/apply-voucher")
    @ResponseBody
    public ResponseEntity<?> applyVoucher(
            @RequestBody ApplyVoucherRequest request,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Vui lòng đăng nhập."));
        }
        String username = authentication.getName();
        String voucherCode = request.getCode(); 

        if (voucherCode == null || voucherCode.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng cung cấp mã voucher."));
        }

        try {
            CartDto updatedCart = promotion.applyVoucher(username, voucherCode); 
            return ResponseEntity.ok(updatedCart); 
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Lỗi hệ thống khi áp dụng voucher."));
        }
    }
    
    @PostMapping("/api/cart/remove-voucher")
    @ResponseBody 
    public ResponseEntity<?> removeVoucher(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Vui lòng đăng nhập."));
        }
        String username = authentication.getName();
        try {
            CartDto updatedCart = promotion.removeVoucher(username); 
            return ResponseEntity.ok(updatedCart); 
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Lỗi hệ thống khi gỡ bỏ voucher."));
        }
    }

    static class AddItemRequest {
        private int quantity;
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }
}