// src/main/java/com/oneshop/service/UserService.java
package com.oneshop.service;

import com.oneshop.dto.ProfileUpdateDto;
import com.oneshop.dto.ShipperCreationDto;
import com.oneshop.entity.Role.RoleName;
import com.oneshop.entity.User;
import org.springframework.security.core.userdetails.UserDetailsService; 
import java.util.Optional;

public interface UserService extends UserDetailsService {

    // --- Các chức năng quản lý User ---
    void register(User user, RoleName roleNameEnum);
    void activate(String email, String otp);
    void forgotPassword(String email);
    void resetPassword(String email, String otp, String newPassword);
    void verifyOtp(String email, String otp, String type);

    User findByUsername(String username);
    Optional<User> findUserByEmail(String email);

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    void save(User user);
    void deleteByEmail(String email);

    void sendOtpForRegistration(String email);

    User updateUserProfile(String username, ProfileUpdateDto profileUpdateDto);
    
    User findById(Long id);
    User updateShipperProfile(String username, ProfileUpdateDto profileUpdateDto, Long shippingCompanyId);
    User createShipperAccountByAdmin(ShipperCreationDto dto);
    
    void updateUserRole(Long userId, String newRoleName);
    void toggleUserStatus(Long userId);
}