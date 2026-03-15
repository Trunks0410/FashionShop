// src/main/java/com/oneshop/controller/UserController.java
package com.oneshop.controller;

import com.oneshop.dto.AddressDTO;
import com.oneshop.dto.ProfileUpdateDto;
import com.oneshop.entity.Address;
import com.oneshop.entity.User;
import com.oneshop.service.AddressService;
import com.oneshop.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/user") // Base path cho các chức năng của User
@PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_VENDOR')") // User và Vendor đều có thể truy cập profile/address
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired private UserService userService;
    @Autowired private AddressService addressService;

    // --- Profile Management ---

    @GetMapping("/profile")
    public String showProfileForm(Model model, @AuthenticationPrincipal User currentUser) {
        logger.debug("Showing profile form for user: {}", currentUser.getUsername());
        ProfileUpdateDto dto = new ProfileUpdateDto();
        dto.setFullName(currentUser.getFullName());
        dto.setEmail(currentUser.getEmail());
        dto.setAddress(currentUser.getAddress()); // Địa chỉ chung (nếu còn dùng)
        dto.setPhoneNumber(currentUser.getPhoneNumber());
        model.addAttribute("profileUpdateDto", dto);
        return "user/profile"; // View templates/user/profile.html
    }

    @PostMapping("/profile/update")
    public String updateProfile(@Valid @ModelAttribute ProfileUpdateDto profileUpdateDto,
                                BindingResult bindingResult,
                                @AuthenticationPrincipal User currentUser,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        logger.info("Attempting to update profile for user: {}", currentUser.getUsername());
        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors updating profile for user {}", currentUser.getUsername());
            model.addAttribute("profileUpdateDto", profileUpdateDto); // Giữ lại dữ liệu
            return "user/profile"; // Quay lại form với lỗi
        }
        try {
            userService.updateUserProfile(currentUser.getUsername(), profileUpdateDto);
            logger.info("Profile updated successfully for user: {}", currentUser.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hồ sơ thành công!");
            return "redirect:/user/profile";
        } catch (IllegalArgumentException e) { // Bắt lỗi email trùng lặp
            logger.warn("Profile update failed for user {}: {}", currentUser.getUsername(), e.getMessage());
            bindingResult.rejectValue("email", "Unique", e.getMessage());
            model.addAttribute("profileUpdateDto", profileUpdateDto);
            return "user/profile";
        } catch (Exception e) { // Bắt lỗi khác
            logger.error("Error updating profile for user {}: {}", currentUser.getUsername(), e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Cập nhật hồ sơ thất bại: " + e.getMessage());
            return "redirect:/user/profile";
        }
    }

    // --- Address Management ---

    @GetMapping("/addresses")
    public String showAddressManagement(Model model, @AuthenticationPrincipal User currentUser) {
        logger.debug("Showing address management for user: {}", currentUser.getUsername());
        try {
            List<Address> addresses = addressService.findByUsernameOrdered(currentUser.getUsername());
            model.addAttribute("addresses", addresses);
        } catch (Exception e) {
            logger.error("Error fetching addresses for user {}: {}", currentUser.getUsername(), e.getMessage(), e);
            model.addAttribute("addresses", List.of());
            model.addAttribute("errorMessage", "Không thể tải danh sách địa chỉ.");
        }
        model.addAttribute("addressDto", new AddressDTO()); // Cho form thêm mới
        return "user/addresses"; // View templates/user/addresses.html
    }

    // API để thêm/sửa địa chỉ (đã có trong AddressController, không cần lặp lại ở đây)
    // Có thể thêm các @PostMapping để xử lý form submit trực tiếp nếu không dùng API/AJAX

    // Ví dụ xử lý xóa địa chỉ bằng form submit (nếu cần)
    @PostMapping("/addresses/delete/{id}")
    public String deleteAddress(@PathVariable Long id, @AuthenticationPrincipal User currentUser, RedirectAttributes redirectAttributes) {
         logger.warn("Attempting to delete address {} for user {}", id, currentUser.getUsername());
         // *** Cần thêm phương thức delete trong AddressService ***
         // try {
         //     addressService.deleteAddress(id, currentUser.getUsername());
         //     redirectAttributes.addFlashAttribute("successMessage", "Xóa địa chỉ thành công!");
         // } catch (Exception e) {
         //     logger.error("Error deleting address {} for user {}: {}", id, currentUser.getUsername(), e.getMessage(), e);
         //     redirectAttributes.addFlashAttribute("errorMessage", "Xóa địa chỉ thất bại: " + e.getMessage());
         // }
         redirectAttributes.addFlashAttribute("errorMessage", "Chức năng xóa địa chỉ chưa được triển khai."); // Tạm thời
        return "redirect:/user/addresses";
    }

     // Ví dụ xử lý đặt làm mặc định bằng form submit (nếu cần)
    @PostMapping("/addresses/set-default/{id}")
    public String setDefaultAddress(@PathVariable Long id, @AuthenticationPrincipal User currentUser, RedirectAttributes redirectAttributes) {
         logger.info("Attempting to set address {} as default for user {}", id, currentUser.getUsername());
         try {
             addressService.setDefaultAddress(id, currentUser.getUsername());
             redirectAttributes.addFlashAttribute("successMessage", "Đặt địa chỉ mặc định thành công!");
         } catch (Exception e) {
              logger.error("Error setting default address {} for user {}: {}", id, currentUser.getUsername(), e.getMessage(), e);
             redirectAttributes.addFlashAttribute("errorMessage", "Đặt địa chỉ mặc định thất bại: " + e.getMessage());
         }
        return "redirect:/user/addresses";
    }

}