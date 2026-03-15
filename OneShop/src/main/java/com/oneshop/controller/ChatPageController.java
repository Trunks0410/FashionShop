// src/main/java/com/oneshop/controller/ChatPageController.java
package com.oneshop.controller;

import com.oneshop.entity.Role.RoleName;
import com.oneshop.entity.User;
import com.oneshop.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model; // <<< Make sure Model is imported
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ChatPageController {

    private final UserService userService;

    public ChatPageController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Mở trang chat giữa USER/SHIPPER ↔ VENDOR
     * URL: /chat/{targetId}
     */
    @GetMapping("/chat/{targetId}")
    @PreAuthorize("hasAnyRole('USER', 'VENDOR', 'SHIPPER')")
    public String openChat(
            @PathVariable Long targetId,
            Authentication auth,
            Model model,
            RedirectAttributes ra) {

        String currentUsername = auth.getName();
        User currentUser = userService.findByUsername(currentUsername);
        User targetUser = userService.findById(targetId);

        if (targetUser == null) {
            ra.addFlashAttribute("errorMessage", "Không tìm thấy người dùng.");
            return "redirect:/contact"; // Hoặc trang phù hợp
        }
        if (currentUser.getId().equals(targetId)) {
             // Redirect vendor back to their main chat page
            if (currentUser.getRole().getName() == RoleName.VENDOR) {
                return "redirect:/vendor/chat";
            }
             // Redirect shipper/user appropriately
            return "redirect:/contact"; // Default redirect
        }

        RoleName currentUserRole = currentUser.getRole().getName();
        RoleName targetUserRole = targetUser.getRole().getName();

        // USER/SHIPPER -> VENDOR
        if (currentUserRole == RoleName.USER || currentUserRole == RoleName.SHIPPER) {
            if (targetUserRole != RoleName.VENDOR) {
                ra.addFlashAttribute("errorMessage", "Bạn chỉ có thể trò chuyện với chủ shop.");
                return "redirect:/contact"; // Or appropriate page
            }
            model.addAttribute("vendor", targetUser);
             // *** ADD currentPage FOR USER/SHIPPER CHAT VIEW IF NEEDED ***
             // model.addAttribute("currentPage", "chat"); // Only if user/shipper view uses the same layout variable
            return "user/chat_user";
        }

        // VENDOR -> USER/SHIPPER
        if (currentUserRole == RoleName.VENDOR) {
            if (targetUserRole != RoleName.USER && targetUserRole != RoleName.SHIPPER) {
                ra.addFlashAttribute("errorMessage", "Bạn chỉ có thể trò chuyện với khách hàng hoặc shipper.");
                return "redirect:/vendor/chat";
            }
            model.addAttribute("user", targetUser);
            // *** ADD currentPage FOR VENDOR CHAT VIEW ***
            model.addAttribute("currentPage", "chat"); // <-- **THÊM DÒNG NÀY**
            return "vendor/chat_vendor"; // Should redirect to the main vendor chat page instead
        }

        // Fallback redirect
        ra.addFlashAttribute("errorMessage", "Không thể mở chat.");
        return "redirect:/contact";
    }

    /**
     * Trang danh sách chat của VENDOR
     */
    @GetMapping("/vendor/chat")
    @PreAuthorize("hasRole('VENDOR')")
    // <<< THÊM Model vào tham số >>>
    public String vendorChatList(Model model) {
        // <<< THÊM DÒNG NÀY >>>
        model.addAttribute("currentPage", "chat"); // Set variable for the layout
        return "vendor/chat_vendor"; // Trả về view vendor/chat_vendor.html
    }
}