package com.oneshop.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oneshop.dto.ShipperCreationDto;
import com.oneshop.entity.Role.RoleName;
import com.oneshop.entity.User;
import com.oneshop.repository.UserRepository;
import com.oneshop.service.UserService;

import jakarta.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminUserController {

    private static final Logger logger = LoggerFactory.getLogger(AdminUserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Xử lý yêu cầu GET /admin/users để hiển thị trang quản lý người dùng.
     * Lọc bỏ người dùng có vai trò ADMIN.
     */
    @GetMapping("/users")
    public String manageUsers(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        logger.debug("Accessing user management page. Keyword: {}", keyword);
        model.addAttribute("currentPage", "admin-users");

        // Đảm bảo DTO cho Modal luôn được thêm vào Model
        if (!model.containsAttribute("shipperCreationDto")) {
            model.addAttribute("shipperCreationDto", new ShipperCreationDto());
        }

        try {
            List<User> allUsers = userRepository.findAll();

            // LỌC BỎ ADMIN
            List<User> manageableUsers = allUsers.stream()
                    .filter(u -> u.getRole() != null && u.getRole().getName() != RoleName.ADMIN)
                    .collect(Collectors.toList());

            List<User> usersToShow;
            if (keyword != null && !keyword.trim().isEmpty()) {
                // Nếu có từ khóa, lọc trên danh sách đã bỏ ADMIN
                String kw = keyword.toLowerCase().trim();
                usersToShow = manageableUsers.stream()
                                      .filter(u -> u.getUsername().toLowerCase().contains(kw) ||
                                                   (u.getFullName() != null && u.getFullName().toLowerCase().contains(kw)) ||
                                                   u.getEmail().toLowerCase().contains(kw))
                                      .collect(Collectors.toList());
                model.addAttribute("keyword", keyword);
                logger.debug("Searching manageable users with keyword: {}", keyword);
            } else {
                usersToShow = manageableUsers;
                logger.debug("Fetching all manageable (non-ADMIN) users.");
            }

            logger.debug("Fetching {} manageable users.", usersToShow.size());
            model.addAttribute("users", usersToShow);
            // Thêm danh sách các RoleName vào model để dùng trong dropdown (loại trừ ADMIN)
            model.addAttribute("availableRoles", List.of(RoleName.USER, RoleName.VENDOR, RoleName.SHIPPER));
            return "admin/users";
        } catch (Exception e) {
            logger.error("Error accessing user management page: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Không thể tải danh sách người dùng.");
            model.addAttribute("users", Collections.emptyList());
            model.addAttribute("availableRoles", List.of(RoleName.USER, RoleName.VENDOR, RoleName.SHIPPER));
            return "admin/users";
        }
    }

    /**
     * Xử lý yêu cầu POST để tạo tài khoản Shipper.
     */
    @PostMapping("/users/create-shipper")
    public String createShipperAccount(@Valid @ModelAttribute("shipperCreationDto") ShipperCreationDto dto,
                                       BindingResult bindingResult,
                                       RedirectAttributes redirectAttributes) {
        logger.info("Admin attempting to create Shipper account for email: {}", dto.getEmail());

        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors during Shipper creation.");
            redirectAttributes.addFlashAttribute("errorMessage", "Dữ liệu tạo tài khoản không hợp lệ. Vui lòng kiểm tra lại.");
            redirectAttributes.addFlashAttribute("shipperCreationDto", dto);
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.shipperCreationDto", bindingResult);
            redirectAttributes.addFlashAttribute("openShipperModal", true);
            return "redirect:/admin/users";
        }

        try {
            User newUser = userService.createShipperAccountByAdmin(dto);
            redirectAttributes.addFlashAttribute("successMessage",
                "Đã tạo tài khoản Shipper **" + newUser.getUsername() + "** thành công! Thông tin đã được gửi qua email.");
        } catch (IllegalArgumentException e) {
            logger.warn("Shipper creation failed (data violation): {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("shipperCreationDto", dto);
            redirectAttributes.addFlashAttribute("openShipperModal", true);
        } catch (RuntimeException e) {
            logger.error("Unexpected error during Shipper creation: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Tạo tài khoản thất bại: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    /**
     * Xử lý yêu cầu POST để thay đổi Quyền (Role) của người dùng.
     * Ngăn chặn việc cấp quyền ADMIN.
     */
    @PostMapping("/users/change-role/{userId}")
    public String changeUserRole(@PathVariable("userId") Long userId,
                                 @RequestParam("newRoleName") String newRoleName,
                                 RedirectAttributes redirectAttributes) {
        logger.info("Attempting to change role for userId: {} to {}", userId, newRoleName);
        try {
            // KIỂM TRA NGĂN CẤP QUYỀN ADMIN
            if (RoleName.ADMIN.name().equalsIgnoreCase(newRoleName)) {
                logger.error("Attempt to grant ADMIN role to user ID: {} DENIED.", userId);
                throw new SecurityException("Không được phép cấp quyền ADMIN.");
            }

            // Sử dụng hàm đã triển khai
            userService.updateUserRole(userId, newRoleName);
            redirectAttributes.addFlashAttribute("successMessage",
                "Đổi quyền thành công cho User #" + userId + " thành **" + newRoleName + "**!");
        } catch (SecurityException | IllegalArgumentException e) {
            logger.error("Security violation or invalid argument changing role for userId {}: {}", userId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error changing role for userId {}: {}", userId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Cấp quyền thất bại: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    /**
     * Xử lý yêu cầu GET để Khóa/Mở khóa (Toggle Status) tài khoản người dùng.
     * Ngăn chặn việc khóa/mở khóa ADMIN.
     */
    @GetMapping("/users/toggle-status/{userId}")
    public String toggleUserStatus(@PathVariable("userId") Long userId,
                                   RedirectAttributes redirectAttributes) {
        logger.info("Attempting to toggle status for userId: {}", userId);
        try {
            // Lấy thông tin user trước khi thay đổi
            User user = userService.findById(userId);
            if (user == null) {
                throw new jakarta.persistence.EntityNotFoundException("Không tìm thấy người dùng với ID: " + userId);
            }

            // KIỂM TRA NGĂN KHÓA ADMIN
            if (user.getRole() != null && user.getRole().getName() == RoleName.ADMIN) {
                logger.error("Attempt to toggle status for ADMIN user ID: {} DENIED.", userId);
                throw new SecurityException("Không được phép khóa/mở khóa tài khoản ADMIN.");
            }

            // Sử dụng hàm đã triển khai để toggle
            userService.toggleUserStatus(userId);
            // Lấy lại user sau khi toggle để hiển thị trạng thái mới
            User updatedUser = userService.findById(userId);
            String status = updatedUser.isActivated() ? "Mở khóa (Hoạt động)" : "Khóa";
            redirectAttributes.addFlashAttribute("successMessage",
                "Đã chuyển trạng thái User #" + userId + " sang **" + status + "**.");
        } catch (SecurityException | jakarta.persistence.EntityNotFoundException e) {
            logger.error("Security violation or user not found toggling status for userId {}: {}", userId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error toggling status for userId {}: {}", userId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Thay đổi trạng thái thất bại: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }
}