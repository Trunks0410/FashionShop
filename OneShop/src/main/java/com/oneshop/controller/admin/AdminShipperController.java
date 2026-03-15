package com.oneshop.controller.admin;

import com.oneshop.dto.ShipperCreationDto;
import com.oneshop.entity.User;
import com.oneshop.service.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminShipperController {

    private static final Logger logger = LoggerFactory.getLogger(AdminShipperController.class);

    @Autowired
    private UserService userService;

    /**
     * Hiển thị form tạo tài khoản Shipper.
     */
    @GetMapping("/shipper/create")
    public String showCreateShipperForm(Model model) {
        model.addAttribute("currentPage", "admin-create-shipper"); // Menu mới
        if (!model.containsAttribute("shipperCreationDto")) {
            model.addAttribute("shipperCreationDto", new ShipperCreationDto());
        }
        return "admin/shipper_create";
    }

    /**
     * Xử lý POST request để tạo tài khoản Shipper, tự động gửi mật khẩu qua email.
     */
    @PostMapping("/shipper/create")
    public String createShipperAccount(@Valid @ModelAttribute("shipperCreationDto") ShipperCreationDto dto,
                                       BindingResult bindingResult,
                                       RedirectAttributes redirectAttributes,
                                       Model model) {
        logger.info("Admin attempting to create Shipper account for email: {}", dto.getEmail());

        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors during Shipper creation.");
            model.addAttribute("currentPage", "admin-create-shipper");
            // Model đã tự chứa lại DTO và BindingResult
            return "admin/shipper_create";
        }

        try {
            User newUser = userService.createShipperAccountByAdmin(dto);
            redirectAttributes.addFlashAttribute("successMessage",
                "Đã tạo tài khoản Shipper **" + newUser.getUsername() + "** thành công! Thông tin đã được gửi qua email.");
        } catch (IllegalArgumentException e) {
            logger.warn("Shipper creation failed (data violation): {}", e.getMessage());
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("currentPage", "admin-create-shipper");
            return "admin/shipper_create"; // Quay lại form với lỗi
        } catch (RuntimeException e) {
            logger.error("Unexpected error during Shipper creation: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Tạo tài khoản thất bại: " + e.getMessage());
        }
        return "redirect:/admin/shipper/create";
    }
}