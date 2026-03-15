package com.oneshop.controller;

import com.oneshop.entity.Order;
import com.oneshop.entity.User;
import com.oneshop.entity.ShippingCompany;
import com.oneshop.entity.OrderStatus;
import com.oneshop.dto.ProfileUpdateDto;
import com.oneshop.service.OrderService;
import com.oneshop.service.UserService;
import com.oneshop.service.ShippingCompanyService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;               
import jakarta.persistence.EntityNotFoundException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/shipper")
@PreAuthorize("hasAuthority('ROLE_SHIPPER')")
public class ShipperController {

    private static final Logger logger = LoggerFactory.getLogger(ShipperController.class);

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

    @Autowired
    private ShippingCompanyService shippingCompanyService;

    /**
     * Hiển thị danh sách đơn hàng được gán cho shipper.
     */
    @GetMapping("/orders")
    public String listAssignedOrders(@AuthenticationPrincipal User shipper, Model model) {
        if (shipper == null || shipper.getUserId() == null) {
            logger.error("Could not get shipper information from AuthenticationPrincipal.");
            return "redirect:/login?error=auth";
        }
        Long shipperId = shipper.getUserId();
        logger.info("Fetching assigned orders for shipperId: {}", shipperId);

        try {
            List<Order> orders = orderService.getAssignedOrders(shipperId);
            Map<String, Long> stats = orderService.getOrderStats(shipperId);

            model.addAttribute("orders", orders);
            model.addAttribute("stats", stats);

            return "shipper/orders";
        } catch (Exception e) {
            logger.error("Error fetching orders/stats for shipperId {}: {}", shipperId, e.getMessage(), e);
            model.addAttribute("errorMessage", "Không thể tải danh sách đơn hàng.");
            model.addAttribute("orders", Collections.emptyList());
            model.addAttribute("stats", Collections.emptyMap());
            return "shipper/orders";
        }
    }

    /**
     * Xử lý khi shipper cập nhật trạng thái đơn hàng.
     */
    @PostMapping("/orders/{id}/update-status")
    public String updateOrderStatus(@PathVariable Long id,
                                    @RequestParam("newStatus") String statusString,
                                    @AuthenticationPrincipal User shipper,
                                    RedirectAttributes redirectAttributes) {
        if (shipper == null || shipper.getUserId() == null) {
            return "redirect:/login?error=auth";
        }
        Long shipperId = shipper.getUserId();
        OrderStatus newStatus;

        try {
            newStatus = OrderStatus.valueOf(statusString.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid status value received: {}", statusString);
            redirectAttributes.addFlashAttribute("errorMessage", "Trạng thái cập nhật không hợp lệ.");
            return "redirect:/shipper/orders";
        }

        logger.info("Shipper {} attempting to update order {} to status {}.", shipperId, id, newStatus);

        try {
            orderService.updateShipperOrderStatus(id, shipperId, newStatus);
            logger.info("Order {} status updated to {} by shipper {}", id, newStatus, shipperId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái đơn hàng #" + id + " thành công!");
        } catch (SecurityException e) {
            logger.warn("Unauthorized attempt by shipper {} to update order {}: {}", shipperId, id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền cập nhật đơn hàng này.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            logger.warn("Failed to update order {} by shipper {}: {}", id, shipperId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể cập nhật trạng thái: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error updating order {} status by shipper {}: {}", id, shipperId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi khi cập nhật trạng thái.");
        }

        return "redirect:/shipper/orders";
    }

    /**
     * Hiển thị form Hồ sơ Shipper và cho phép chọn Đơn vị Vận chuyển.
     */
    @GetMapping("/profile")
    public String showProfileForm(Model model, @AuthenticationPrincipal User currentUser) {
        model.addAttribute("currentPage", "profile");

        ProfileUpdateDto dto = new ProfileUpdateDto();
        dto.setFullName(currentUser.getFullName());
        dto.setEmail(currentUser.getEmail());
        dto.setAddress(currentUser.getAddress());
        dto.setPhoneNumber(currentUser.getPhoneNumber());

        // Lấy danh sách các công ty đang hoạt động cho combobox
        List<ShippingCompany> activeCompanies = shippingCompanyService.findActiveCompanies();

        model.addAttribute("profileUpdateDto", dto);
        model.addAttribute("currentCompanyId", currentUser.getShippingCompany() != null ? 
            currentUser.getShippingCompany().getShippingId() : null);
        model.addAttribute("activeCompanies", activeCompanies);

        return "shipper/profile";
    }

    /**
     * Xử lý cập nhật Hồ sơ và Đơn vị Vận chuyển.
     */
    @PostMapping("/profile/update")
    public String updateProfile(@Valid @ModelAttribute ProfileUpdateDto profileUpdateDto,
                                BindingResult bindingResult,
                                @RequestParam(value = "shippingCompanyId", required = false) Long shippingCompanyId,
                                @AuthenticationPrincipal User currentUser,
                                RedirectAttributes redirectAttributes,
                                Model model) {

        if (bindingResult.hasErrors()) {
            List<ShippingCompany> activeCompanies = shippingCompanyService.findActiveCompanies();
            model.addAttribute("activeCompanies", activeCompanies);
            model.addAttribute("currentCompanyId", shippingCompanyId);
            return "shipper/profile";
        }

        try {
            userService.updateShipperProfile(currentUser.getUsername(), profileUpdateDto, shippingCompanyId);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật Hồ sơ Shipper thành công!");
            return "redirect:/shipper/profile";

        } catch (IllegalArgumentException | EntityNotFoundException e) {
            List<ShippingCompany> activeCompanies = shippingCompanyService.findActiveCompanies();
            model.addAttribute("activeCompanies", activeCompanies);
            model.addAttribute("currentCompanyId", shippingCompanyId);
            model.addAttribute("errorMessage", e.getMessage());
            return "shipper/profile";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cập nhật thất bại: " + e.getMessage());
            return "redirect:/shipper/profile";
        }
    }
}