package com.oneshop.controller; // Or your controller package

import com.oneshop.dto.CartDto;
import com.oneshop.dto.CartItemDto;
import com.oneshop.entity.Address;
import com.oneshop.service.AddressService;
import com.oneshop.service.CartService;
import com.oneshop.service.impl.PromotionServiceImpl; // Import PromotionServiceImpl để dùng các hằng số

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class CheckoutController {

	private static final BigDecimal INITIAL_ESTIMATED_SHIPPING_FEE = new BigDecimal("30000");

    @Autowired
    private CartService cartService;

    @Autowired
    private AddressService addressService;

    @GetMapping("/pay") // Or "/checkout" if you prefer that URL
    public String showCheckoutPage(
            @RequestParam(name = "variantIds", required = false) List<Long> variantIds, 
            Authentication authentication,
            Model model) {

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/login"; 
        }
        
        if (variantIds == null || variantIds.isEmpty()) {
            return "redirect:/cart"; 
        }

        String username = authentication.getName();

        // 1. Get User Addresses
        List<Address> addresses = addressService.findByUsernameOrdered(username);
        Address defaultAddress = addresses.stream()
                                         .filter(addr -> Boolean.TRUE.equals(addr.getIsDefault()))
                                         .findFirst()
                                         .orElse(addresses.isEmpty() ? null : addresses.get(0)); 

        // 2. Get the full cart and filter selected items
        // CartDto hiện tại đã chứa thông tin voucher từ Session (nhờ CartService đã sửa)
        CartDto fullCart = cartService.getCartItems(username);
        Map<Long, CartItemDto> selectedItemsMap = fullCart.getItems().entrySet().stream()
                .filter(entry -> variantIds.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                
        List<CartItemDto> selectedItemsList = List.copyOf(selectedItemsMap.values());

        // 3. Tính toán Totals dựa trên item được chọn
        BigDecimal subtotal = selectedItemsMap.values().stream()
                .map(CartItemDto::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal estimatedShippingCost = INITIAL_ESTIMATED_SHIPPING_FEE; // Phí ước tính ban đầu
        BigDecimal discountAmount = BigDecimal.ZERO;
        boolean isFreeShipping = false; // Cờ freeship

        String voucherCode = fullCart.getAppliedVoucherCode();
        String voucherType = fullCart.getAppliedVoucherTypeCode();
        BigDecimal voucherValue = fullCart.getAppliedVoucherValue();

        if (voucherCode != null && subtotal.compareTo(BigDecimal.ZERO) > 0) {
            if ("PERCENTAGE".equalsIgnoreCase(voucherType) && voucherValue != null) {
                discountAmount = subtotal.multiply(voucherValue)
                                         .divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP);
            } else if ("FIXED_AMOUNT".equalsIgnoreCase(voucherType) && voucherValue != null) {
                discountAmount = voucherValue.min(subtotal);
            } else if ("FREE_SHIPPING".equalsIgnoreCase(voucherType)) {
                // Nếu là freeship, phí ước tính = 0
                estimatedShippingCost = BigDecimal.ZERO;
                isFreeShipping = true;
                discountAmount = BigDecimal.ZERO; // Freeship không giảm vào subtotal
            }
            discountAmount = discountAmount.min(subtotal);
        }

        // Tính tổng tiền TẠM TÍNH (bao gồm phí ship ước tính)
        BigDecimal estimatedGrandTotal = subtotal
                                .subtract(discountAmount)
                                .add(estimatedShippingCost); // Cộng phí ship ước tính
        estimatedGrandTotal = estimatedGrandTotal.max(BigDecimal.ZERO);


        // 4. Add data to the model
        model.addAttribute("addresses", addresses);
        model.addAttribute("defaultAddress", defaultAddress);
        model.addAttribute("selectedItems", selectedItemsList);
        model.addAttribute("selectedVariantIds", variantIds);
        
        // --- THÔNG TIN TỔNG KẾT ĐÃ CẬP NHẬT ---
        model.addAttribute("subtotal", subtotal);
        // ** KHÔNG truyền shippingCost cố định nữa **
        // model.addAttribute("shippingCost", shippingCost);
        model.addAttribute("grandTotal", estimatedGrandTotal);
        
        // --- THÔNG TIN VOUCHER ĐỂ HIỂN THỊ ---
        model.addAttribute("discountAmount", discountAmount); // Số tiền giảm
        model.addAttribute("appliedVoucherCode", voucherCode); // Mã áp dụng
        model.addAttribute("isFreeShipping", isFreeShipping); // Cờ Free Ship
        // ----------------------------------------

        return "user/pay"; // Your Thymeleaf template name (e.g., pay.html)
    }

    // --- API & Helper Endpoints ---

    @GetMapping("/api/addresses/{id}")
    public ResponseEntity<Address> getAddressById(@PathVariable Long id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(401).build();
        }
        String username = authentication.getName();
        return addressService.findByUsernameOrdered(username).stream()
                .filter(a -> a.getAddressId().equals(id))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Add POST mapping later for /placeOrder
}