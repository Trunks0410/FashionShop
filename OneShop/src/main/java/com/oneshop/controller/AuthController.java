// src/main/java/com/oneshop/controller/AuthController.java
package com.oneshop.controller;

import com.oneshop.dto.RegisterRequestDto;
import com.oneshop.entity.Role.RoleName;
import com.oneshop.entity.User;
// Bỏ import AuthService
import com.oneshop.service.UserService;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
// Bỏ import HttpStatus, ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils; // Thêm import này

import java.nio.charset.StandardCharsets; // Thêm import này
// Bỏ import Map, HashMap

@Controller
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    // Bỏ inject AuthService

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // --- Các trang View (giữ nguyên) ---

    @GetMapping("/login")
    public String showLoginForm() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        // Form này dùng để đăng ký USER
        model.addAttribute("registerRequestDto", new RegisterRequestDto());
        return "auth/register";
    }

    @GetMapping("/verify-otp")
    public String verifyOtpForm(@RequestParam String type, @RequestParam String email, Model model) {
        model.addAttribute("type", type);
        model.addAttribute("email", email);
        return "guest/verify-otp";
    }

    @GetMapping("/forgot")
    public String forgotForm() {
        return "guest/forgot";
    }

    @GetMapping("/reset-password")
    public String resetForm(@RequestParam String email, @RequestParam String otp, Model model) {
        model.addAttribute("email", email);
        model.addAttribute("otp", otp);
        return "guest/reset-password";
    }

    // --- Xử lý Form Submit ---

    // Xử lý đăng ký USER (từ form /register)
    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("registerRequestDto") RegisterRequestDto registerRequestDto,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        logger.info("Attempting to register USER with username: {}", registerRequestDto.getUsername());

        // Kiểm tra mật khẩu khớp
        if (registerRequestDto.getPassword() != null && !registerRequestDto.getPassword().equals(registerRequestDto.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "Match", "Mật khẩu xác nhận không khớp.");
        }

        // Kiểm tra lỗi validation
        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors during user registration for {}", registerRequestDto.getUsername());
            model.addAttribute("registerRequestDto", registerRequestDto);
            return "auth/register"; // Quay lại form hiển thị lỗi
        }

        try {
            // Tạo đối tượng User từ DTO
            User newUser = new User();
            newUser.setFullName(registerRequestDto.getFullName().trim());
            newUser.setEmail(registerRequestDto.getEmail().trim().toLowerCase());
            newUser.setUsername(registerRequestDto.getUsername().trim());
            newUser.setPassword(registerRequestDto.getPassword()); // Service sẽ mã hóa
            newUser.setPhoneNumber(registerRequestDto.getPhoneNumber().trim());

            // Gọi service để đăng ký USER, service sẽ kiểm tra trùng lặp và gửi OTP
            userService.register(newUser, RoleName.USER);
            logger.info("User registration submitted for {}, redirecting to OTP verification.", registerRequestDto.getEmail());

            // Chuyển hướng đến trang nhập OTP
            return "redirect:/verify-otp?type=REGISTRATION&email=" + UriUtils.encode(registerRequestDto.getEmail().trim(), StandardCharsets.UTF_8);

        } catch (IllegalArgumentException e) {
            // Bắt lỗi cụ thể từ service (username/email tồn tại)
            logger.warn("User registration failed for {}: {}", registerRequestDto.getUsername(), e.getMessage());
            if (e.getMessage().contains("Tên đăng nhập")) {
                 bindingResult.rejectValue("username", "Unique", e.getMessage());
            } else if (e.getMessage().contains("Email")) {
                 bindingResult.rejectValue("email", "Unique", e.getMessage());
            } else {
                 model.addAttribute("errorMessage", e.getMessage());
            }
            model.addAttribute("registerRequestDto", registerRequestDto);
            return "auth/register"; // Quay lại form hiển thị lỗi
        } catch (RuntimeException e) { // Bắt lỗi gửi OTP hoặc lỗi khác
            logger.error("Error during user registration or sending OTP for {}: {}", registerRequestDto.getEmail(), e.getMessage(), e);
            model.addAttribute("errorMessage", "Đã xảy ra lỗi khi gửi mã xác thực. Vui lòng thử lại.");
            model.addAttribute("registerRequestDto", registerRequestDto);
            return "auth/register";
        }
    }

    // --- Xử lý xác thực OTP (giữ nguyên) ---
    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestParam String type,
                            @RequestParam String email,
                            @RequestParam String otp,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        logger.info("Attempting to verify OTP type '{}' for email: {}", type, email);
        try {
            if ("REGISTRATION".equals(type)) {
                userService.activate(email, otp);
                logger.info("Account activation successful for {}", email);
                redirectAttributes.addFlashAttribute("successMessage", "Xác thực tài khoản thành công! Vui lòng đăng nhập.");
                return "redirect:/login";
            } else if ("FORGOT".equals(type)) {
                userService.verifyOtp(email, otp, type);
                logger.info("Forgot password OTP verified for {}", email);
                return "redirect:/reset-password?email=" + UriUtils.encode(email, StandardCharsets.UTF_8) + "&otp=" + UriUtils.encode(otp, StandardCharsets.UTF_8);
            } else {
                logger.warn("Invalid OTP verification type received: {}", type);
                model.addAttribute("error", "Loại xác thực không hợp lệ!");
                model.addAttribute("type", type);
                model.addAttribute("email", email);
                return "guest/verify-otp";
            }
        } catch (RuntimeException e) {
            logger.warn("OTP verification failed for {} (type {}): {}", email, type, e.getMessage());
            model.addAttribute("type", type);
            model.addAttribute("email", email);
            model.addAttribute("error", e.getMessage());
            return "guest/verify-otp";
        }
    }

    // --- Xử lý gửi OTP quên mật khẩu (giữ nguyên) ---
    @PostMapping("/forgot")
    public String forgotPassword(@RequestParam String email, RedirectAttributes redirectAttributes) {
        logger.info("Forgot password request for email: {}", email);
        try {
            userService.forgotPassword(email);
            logger.info("Forgot password OTP sent successfully to {}", email);
            return "redirect:/verify-otp?type=FORGOT&email=" + UriUtils.encode(email, StandardCharsets.UTF_8);
        } catch (RuntimeException e) {
            logger.warn("Forgot password request failed for {}: {}", email, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/forgot";
        }
    }

    // --- Xử lý đặt lại mật khẩu (giữ nguyên) ---
    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String email,
                                @RequestParam String otp,
                                @RequestParam String newPassword,
                                @RequestParam String confirmPassword,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        logger.info("Attempting to reset password for email: {}", email);
        if (newPassword == null || !newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Mật khẩu mới và xác nhận không khớp!");
            model.addAttribute("email", email);
            model.addAttribute("otp", otp);
            return "guest/reset-password";
        }
        if (newPassword.length() < 6) {
             model.addAttribute("error", "Mật khẩu mới phải có ít nhất 6 ký tự!");
             model.addAttribute("email", email);
             model.addAttribute("otp", otp);
             return "guest/reset-password";
        }
        try {
            userService.resetPassword(email, otp, newPassword);
            logger.info("Password reset successful for {}", email);
            redirectAttributes.addFlashAttribute("successMessage", "Đặt lại mật khẩu thành công! Vui lòng đăng nhập.");
            return "redirect:/login";
        } catch (RuntimeException e) {
            logger.warn("Password reset failed for {}: {}", email, e.getMessage());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("email", email);
            return "guest/reset-password";
        }
    }
}