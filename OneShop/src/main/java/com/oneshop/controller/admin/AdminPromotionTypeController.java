package com.oneshop.controller.admin;

import com.oneshop.entity.PromotionTypeEntity;
import com.oneshop.service.PromotionTypeService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

import java.util.List;
import java.util.Collections;
import org.springframework.util.StringUtils;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminPromotionTypeController {

    private static final Logger logger = LoggerFactory.getLogger(AdminPromotionTypeController.class);

    @Autowired
    private PromotionTypeService promotionTypeService;

    @GetMapping("/promotion-types")
    public String listPromotionTypes(Model model) {
        model.addAttribute("currentPage", "admin-promotion-types");
        logger.debug("Accessing admin promotion type management page.");
        try {
            List<PromotionTypeEntity> promotionTypes = promotionTypeService.findAll();
            model.addAttribute("promotionTypes", promotionTypes);
        } catch (Exception e) {
            logger.error("Error fetching promotion types for admin: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Không thể tải danh sách Loại Khuyến mãi.");
            model.addAttribute("promotionTypes", Collections.emptyList());
        }
        if (!model.containsAttribute("newPromotionType")) {
            model.addAttribute("newPromotionType", new PromotionTypeEntity());
        }
        return "admin/promotion_type_management"; // Cần tạo template này
    }

    @PostMapping("/promotion-types/add")
    public String addPromotionType(@Valid @ModelAttribute("newPromotionType") PromotionTypeEntity newType,
                                 BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {
        logger.info("Admin attempting to add new promotion type: {}", newType.getCode());
        model.addAttribute("currentPage", "admin-promotion-types");

        try {
            if (bindingResult.hasErrors()) {
                 logger.warn("Validation errors during admin add promotion type.");
                 throw new RuntimeException("Dữ liệu không hợp lệ.");
            }
             if (!StringUtils.hasText(newType.getCode())) {
                bindingResult.rejectValue("code", "NotBlank", "Vui lòng chọn mã loại.");
                throw new RuntimeException("Chưa chọn mã loại.");
            }
             if (!StringUtils.hasText(newType.getName())) {
                bindingResult.rejectValue("name", "NotBlank", "Tên hiển thị không được để trống.");
                throw new RuntimeException("Chưa nhập tên hiển thị.");
            }
             newType.setName(newType.getName().trim());


            promotionTypeService.savePromotionType(newType);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm loại khuyến mãi **" + newType.getName() + "**!");
            return "redirect:/admin/promotion-types";

        } catch (RuntimeException e) {
            // Lỗi trùng lặp hoặc lỗi khác
            logger.warn("Admin add promotion type failed: {}", e.getMessage());
            // Tải lại dữ liệu cần thiết cho form
            model.addAttribute("promotionTypes", promotionTypeService.findAll());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("newPromotionType", newType);
            return "redirect:/admin/promotion-types";
        } catch (Exception e) {
             logger.error("Unexpected error during admin add promotion type: {}", e.getMessage(), e);
             redirectAttributes.addFlashAttribute("errorMessage", "Lỗi không xác định khi thêm loại khuyến mãi: " + e.getMessage());
             return "redirect:/admin/promotion-types";
        }
    }

    @PostMapping("/promotion-types/delete/{id}")
    public String deletePromotionType(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        logger.warn("Admin attempting to delete promotion type ID: {}", id);
        try {
            promotionTypeService.deletePromotionType(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa loại khuyến mãi thành công!");
        } catch (RuntimeException e) {
            logger.error("Admin delete promotion type failed for ID {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during admin delete promotion type: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi không xác định khi xóa loại khuyến mãi.");
        }
        return "redirect:/admin/promotion-types";
    }
}