package com.oneshop.controller; // Or your config package

import com.oneshop.dto.CartDto;
import com.oneshop.service.CartService; // Import your CartService
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private CartService cartService; // Inject the CartService

    /**
     * This method runs before controllers and adds the "cart" 
     * attribute to the model for ALL requests.
     */
    @ModelAttribute("cart") // The attribute name in the model will be "cart"
    public CartDto addCartToModel() {
        // Get the current user's authentication info
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Check if the user is logged in (authenticated) and not anonymous
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            String username = authentication.getName();
            // Get CartDto from the service for the logged-in user
            return cartService.getCartItems(username);
        }

        // If the user is not logged in, return an empty CartDto
        // This prevents Thymeleaf errors when accessing cart.items, etc.
        return new CartDto(); 
    }
}