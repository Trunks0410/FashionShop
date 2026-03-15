package com.oneshop.controller.admin;

import com.oneshop.dto.PromotionDto;
import com.oneshop.entity.Promotion;
import com.oneshop.entity.PromotionTypeEntity;
import com.oneshop.service.PromotionService;
import com.oneshop.service.PromotionTypeService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;

import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminPromotionController {

    private static final Logger logger = LoggerFactory.getLogger(AdminPromotionController.class);

    @Autowired
    private PromotionService promotionService;

    @Autowired
    private PromotionTypeService promotionTypeService;

    @GetMapping("/promotions")
    public String listPromotions(Model model, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        model.addAttribute("currentPage", "admin-promotions");
        logger.debug("Accessing admin promotions list page.");
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("endDate").descending());
            Page<Promotion> promotionPage = promotionService.findActiveAndUpcomingPromotions(pageable);
            
            model.addAttribute("promotionPage", promotionPage);
            model.addAttribute("currentPromoPage", page);
        } catch (Exception e) {
            logger.error("Error fetching promotions for admin: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Không thể tải danh sách Khuyến mãi.");
            model.addAttribute("promotionPage", Page.empty(PageRequest.of(page, size)));
        }
        return "admin/promotion_list";
    }

    @GetMapping("/promotions/add")
    public String addPromotionForm(Model model) {
        model.addAttribute("currentPage", "admin-promotions");
        try {
            model.addAttribute("promotionDto", new PromotionDto());
            List<PromotionTypeEntity> types = promotionTypeService.findAll();
            model.addAttribute("promotionTypes", types);
            model.addAttribute("isEditMode", false);
        } catch (Exception e) {
            logger.error("Error loading promotion types for admin add form: {}", e.getMessage());
            model.addAttribute("promotionTypes", Collections.emptyList());
            model.addAttribute("errorMessage", "Lỗi tải loại khuyến mãi.");
        }
        return "admin/promotion_add";
    }

    @GetMapping("/promotions/edit/{id}")
    public String editPromotionForm(@PathVariable("id") Long promotionId, Model model, RedirectAttributes redirectAttributes) {
        model.addAttribute("currentPage", "admin-promotions");
        logger.debug("Admin showing edit promotion form for ID: {}", promotionId);
        try {
            // Sử dụng findPromotionById thay vì findByDiscountCode
            Promotion promotion = promotionService.findPromotionById(promotionId)
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khuyến mãi."));

            PromotionDto dto = mapPromotionToDto(promotion);

            model.addAttribute("promotionDto", dto);
            List<PromotionTypeEntity> types = promotionTypeService.findAll();
            model.addAttribute("promotionTypes", types);
            model.addAttribute("isEditMode", true);
            return "admin/promotion_add";
        } catch (EntityNotFoundException e) {
             logger.warn("Promotion edit failed: {}", e.getMessage());
             redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
             return "redirect:/admin/promotions";
        } catch (Exception e) {
            logger.error("Error loading edit promotion form for ID {}: {}", promotionId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể tải thông tin khuyến mãi để sửa: " + e.getMessage());
            return "redirect:/admin/promotions";
        }
    }

    @PostMapping("/promotions/save")
    public String saveOrUpdatePromotion(@Valid @ModelAttribute("promotionDto") PromotionDto promotionDto,
                                        BindingResult bindingResult,
                                        RedirectAttributes redirectAttributes,
                                        Model model) {
        boolean isEditMode = promotionDto.getId() != null;
        logger.info("Admin saving promotion (edit={}): {}", isEditMode, promotionDto.getDiscountCode());

        try {
            if (bindingResult.hasErrors()) {
                 logger.warn("Admin validation errors saving promotion.");
                 model.addAttribute("promotionTypes", promotionTypeService.findAll());
                 model.addAttribute("isEditMode", isEditMode);
                 return "admin/promotion_add";
            }

            Long adminShopId = 1L; // Shop ID mặc định

            if (isEditMode) {
                // Load lại Entity cũ bằng ID để lấy ShopId
                Promotion existingPromo = promotionService.findPromotionById(promotionDto.getId())
                                        .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khuyến mãi để cập nhật."));
                adminShopId = existingPromo.getShop().getId(); // Lấy ShopId từ entity cũ
                promotionService.updatePromotion(promotionDto.getId(), promotionDto, adminShopId); 
            } else {
                // Tạo mới, gán vào Shop mặc định ID 1
                promotionService.createPromotion(promotionDto, adminShopId); 
            }
            
            redirectAttributes.addFlashAttribute("successMessage", (isEditMode ? "Cập nhật" : "Tạo") + " khuyến mãi thành công!");
            return "redirect:/admin/promotions";

        } catch (Exception e) {
            logger.error("Admin save promotion failed: {}", e.getMessage(), e);
            model.addAttribute("promotionTypes", promotionTypeService.findAll());
            model.addAttribute("isEditMode", isEditMode);
            model.addAttribute("errorMessage", (isEditMode ? "Cập nhật" : "Tạo") + " khuyến mãi thất bại: " + e.getMessage());
            return "admin/promotion_add";
        }
    }

    @PostMapping("/promotions/delete/{id}")
    public String deletePromotion(@PathVariable("id") Long promotionId, RedirectAttributes redirectAttributes) {
        logger.warn("Admin deleting promotion ID: {}", promotionId);
        try {
            // Load lại Entity cũ bằng ID để lấy ShopId
            Promotion promotionToDelete = promotionService.findPromotionById(promotionId)
                                        .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khuyến mãi để xóa."));
            Long shopId = promotionToDelete.getShop().getId(); // Lấy ShopId từ entity cũ
            
            promotionService.deletePromotion(promotionId, shopId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa khuyến mãi thành công!");
        } catch (Exception e) {
            logger.error("Admin delete promotion failed for ID {}: {}", promotionId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Xóa thất bại: " + e.getMessage());
        }
        return "redirect:/admin/promotions";
    }

    // Helper function
    private PromotionDto mapPromotionToDto(Promotion promotion) {
        PromotionDto dto = new PromotionDto();
        dto.setId(promotion.getId());
        dto.setCampaignName(promotion.getCampaignName());
        dto.setDiscountCode(promotion.getDiscountCode());
        if (promotion.getType() != null) {
            dto.setPromotionTypeId(promotion.getType().getId());
        }
        dto.setDiscountValue(promotion.getValue());
        dto.setStartDate(promotion.getStartDate());
        dto.setEndDate(promotion.getEndDate());
        return dto;
    }
}