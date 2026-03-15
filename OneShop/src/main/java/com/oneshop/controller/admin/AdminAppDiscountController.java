package com.oneshop.controller.admin;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oneshop.entity.Shop;
import com.oneshop.service.ShopService;

@Controller
@RequestMapping("/admin")
public class AdminAppDiscountController {

	private static final Logger logger = LoggerFactory.getLogger(AdminAppDiscountController.class);

	@Autowired
	private ShopService shopService;

	@GetMapping("/app-discounts")
	public String listApprovedShops(Model model) {
		model.addAttribute("currentPage", "admin-discounts");
		logger.debug("Accessing app discounts management page.");
		try {
			List<Shop> approvedShops = shopService.getApprovedShops();
			model.addAttribute("shops", approvedShops);
		} catch (Exception e) {
            logger.error("Error fetching approved shops: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Không thể tải danh sách Shop.");
            model.addAttribute("shops", List.of());
        }
		return "admin/app-discounts";
	}

	@PostMapping("/app-discounts/update")
	public String updateAppDiscount(@RequestParam("shopId") Long shopId,
			@RequestParam("newCommissionRatePercent") BigDecimal newCommissionRatePercent,
			RedirectAttributes redirectAttributes) {
		logger.info("Attempting to update commission for shopId: {} to {}%", shopId, newCommissionRatePercent);

		try {
			if (newCommissionRatePercent == null || newCommissionRatePercent.compareTo(BigDecimal.ZERO) < 0
					|| newCommissionRatePercent.compareTo(new BigDecimal("100")) > 0) {
				logger.warn("Invalid commission percentage received: {}", newCommissionRatePercent);
				redirectAttributes.addFlashAttribute("message",
						"Chiết khấu không hợp lệ! Phải là số từ 0 đến 100.");
				redirectAttributes.addFlashAttribute("status", "danger");
				return "redirect:/admin/app-discounts";
			}
			BigDecimal newCommissionRate = newCommissionRatePercent.divide(new BigDecimal("100"), 4,
					BigDecimal.ROUND_HALF_UP);

			Shop updatedShop = shopService.updateShopCommissionRate(shopId, newCommissionRate);

			if (updatedShop != null) {
				logger.info("Commission updated successfully for shop: {}", updatedShop.getName());
				redirectAttributes.addFlashAttribute("message",
						"Cập nhật chiết khấu cho cửa hàng **" + updatedShop.getName() + "** thành công!");
				redirectAttributes.addFlashAttribute("status", "success");
			} else {
				logger.error("Shop not found with ID {} during commission update.", shopId);
				redirectAttributes.addFlashAttribute("message", "Lỗi: Không tìm thấy cửa hàng với ID " + shopId + ".");
				redirectAttributes.addFlashAttribute("status", "danger");
			}

		} catch (Exception e) {
			logger.error("Error updating commission for shopId {}: {}", shopId, e.getMessage(), e);
			redirectAttributes.addFlashAttribute("message", "Đã xảy ra lỗi trong quá trình cập nhật chiết khấu: " + e.getMessage());
			redirectAttributes.addFlashAttribute("status", "danger");
		}
		return "redirect:/admin/app-discounts";
	}
}