package com.oneshop.controller;

import com.oneshop.dto.ShopDto;
import com.oneshop.entity.User;
import com.oneshop.enums.ShopStatus;
import com.oneshop.service.ShopService;
import com.oneshop.service.UserService;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/shop")
@PreAuthorize("isAuthenticated()")
public class ShopRegistrationController {

    private static final Logger logger = LoggerFactory.getLogger(ShopRegistrationController.class);

    @Autowired
    private ShopService shopService;

    @Autowired
    private UserService userService;

    @GetMapping("/register")
    public String showRegistrationForm(Model model, Authentication authentication) {
        String username = authentication.getName();
        User currentUser = userService.findByUsername(username);

        if (currentUser.getShop() != null) {
            logger.warn("User {} already has a shop (status: {}), redirecting from registration form.", username, currentUser.getShop().getStatus());
             if (currentUser.getShop().getStatus() == ShopStatus.APPROVED) {
                 return "redirect:/vendor/dashboard";
             } else {
                 model.addAttribute("infoMessage", "Bạn đã gửi yêu cầu đăng ký shop và đang chờ duyệt.");
                  return "user/shop_registration_status";
             }
        }

        model.addAttribute("shopDto", new ShopDto());
        return "user/shop_register";
    }

    @PostMapping("/register")
    public String processRegistration(@Valid @ModelAttribute("shopDto") ShopDto shopDto,
                                      BindingResult bindingResult,
                                      Authentication authentication,
                                      RedirectAttributes redirectAttributes,
                                      Model model) {
        String username = authentication.getName();
        User currentUser = userService.findByUsername(username);

        if (currentUser.getShop() != null) {
             logger.warn("User {} tried to submit registration form but already has a shop.", username);
             redirectAttributes.addFlashAttribute("errorMessage", "Bạn đã đăng ký shop rồi.");
             return "redirect:/";
        }

        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors during shop registration for user {}", username);
            return "user/shop_register";
        }

        try {
            shopService.registerShop(shopDto, currentUser.getId());
            logger.info("Shop registration request submitted successfully for user {}", username);
            redirectAttributes.addFlashAttribute("successMessage", "Yêu cầu đăng ký shop của bạn đã được gửi. Vui lòng chờ quản trị viên duyệt.");
            return "redirect:/";

        } catch (RuntimeException e) {
            logger.error("Error processing shop registration for user {}: {}", username, e.getMessage(), e);
            model.addAttribute("errorMessage", "Đăng ký shop thất bại: " + e.getMessage());
            model.addAttribute("shopDto", shopDto);
            return "user/shop_register";
        }
    }
}