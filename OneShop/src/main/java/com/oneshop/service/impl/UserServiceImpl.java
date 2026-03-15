package com.oneshop.service.impl;

import com.oneshop.dto.ProfileUpdateDto;
import com.oneshop.dto.ShipperCreationDto;
import com.oneshop.entity.Otp;
import com.oneshop.entity.Role;
import com.oneshop.entity.Role.RoleName;
import com.oneshop.entity.User;
import com.oneshop.repository.OtpRepository;
import com.oneshop.repository.RoleRepository;
import com.oneshop.repository.UserRepository;
import com.oneshop.service.EmailService;
import com.oneshop.service.OtpService;
import com.oneshop.service.UserService;
import com.oneshop.entity.ShippingCompany; 
import com.oneshop.repository.ShippingCompanyRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

import jakarta.persistence.EntityNotFoundException;

@Service("userService")
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private OtpRepository otpRepository;
    @Autowired @Lazy private PasswordEncoder passwordEncoder;
    @Autowired private EmailService emailService;
    @Autowired private OtpService otpService;
    @Autowired private ShippingCompanyRepository shippingCompanyRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(User user, RoleName roleNameEnum) {
        if (userRepository.findByUsername(user.getUsername()).isPresent() || userRepository.findByEmail(user.getEmail()).isPresent()) {
            logger.warn("Email hoặc username đã tồn tại: {} / {}", user.getEmail(), user.getUsername());
            throw new IllegalArgumentException("Email hoặc username đã tồn tại!");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        Role role = roleRepository.findByName(roleNameEnum)
                .orElseThrow(() -> {
                    logger.error("Không tìm thấy Role với tên: {}", roleNameEnum);
                    return new RuntimeException("Vai trò hệ thống không hợp lệ: " + roleNameEnum);
                });
        user.setRole(role);
        user.setActivated(false);
        User savedUser = userRepository.save(user);
        try {
            otpService.generateAndSendOtp(savedUser.getEmail(), "REGISTRATION");
            logger.info("Đăng ký thành công và gửi OTP cho: {}", savedUser.getEmail());
        } catch (Exception e) {
            logger.error("Lỗi gửi OTP cho email {}: {}", savedUser.getEmail(), e.getMessage());
            throw new RuntimeException("Không thể gửi OTP xác thực: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void activate(String email, String otp) {
        otpService.verifyOtp(email, otp, "REGISTRATION");
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email: " + email));
        if (user.isActivated()) {
             logger.warn("Tài khoản đã được kích hoạt trước đó: {}", email);
             return;
        }
        user.setActivated(true);
        userRepository.save(user);
        logger.info("Kích hoạt tài khoản thành công cho: {}", email);
    }

    @Override
    public void forgotPassword(String email) {
        userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email: " + email));
        otpService.generateAndSendOtp(email, "FORGOT");
        logger.info("Đã gửi OTP quên mật khẩu tới: {}", email);
    }

    @Override
    @Transactional
    public void resetPassword(String email, String otp, String newPassword) {
        otpService.verifyOtp(email, otp, "FORGOT");
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email: " + email));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        logger.info("Đặt lại mật khẩu thành công cho: {}", email);
    }

    @Override
    public void verifyOtp(String email, String otp, String type) {
        otpService.verifyOtp(email, otp, type);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        logger.debug("Attempting to load user by username or email: {}", usernameOrEmail);

        Optional<User> userOptional;

        userOptional = userRepository.findByUsername(usernameOrEmail);

        if (userOptional.isEmpty()) {
            logger.debug("User not found by username '{}', trying by email...", usernameOrEmail);
            userOptional = userRepository.findByEmail(usernameOrEmail);
            if(userOptional.isPresent()){
                logger.debug("User found by email: {}", usernameOrEmail);
            }
        } else {
             logger.debug("User found by username: {}", usernameOrEmail);
        }
        return userOptional.orElseThrow(() -> {
            logger.warn("User not found with username or email: {}", usernameOrEmail);
            return new UsernameNotFoundException("Không tìm thấy người dùng với username hoặc email: " + usernameOrEmail);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
               .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng: " + username));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    @Override
    @Transactional
    public void save(User user) {
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteByEmail(String email) {
        userRepository.findByEmail(email)
                .ifPresent(user -> {
                    otpRepository.findByUser_IdAndType(user.getId(), "REGISTRATION").ifPresent(otpRepository::delete);
                    otpRepository.findByUser_IdAndType(user.getId(), "FORGOT").ifPresent(otpRepository::delete);
                    userRepository.delete(user);
                    logger.info("Đã xóa người dùng với email: {}", email);
                });
    }

    @Override
    public void sendOtpForRegistration(String email) {
         userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email: " + email + " để gửi OTP đăng ký."));
        otpService.generateAndSendOtp(email, "REGISTRATION");
    }

    @Override
    @Transactional
    public User updateUserProfile(String username, ProfileUpdateDto profileUpdateDto) {
        User user = findByUsername(username);

        Optional<User> userByNewEmail = userRepository.findByEmail(profileUpdateDto.getEmail().trim().toLowerCase());
        if (userByNewEmail.isPresent() && !userByNewEmail.get().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Email đã được sử dụng bởi tài khoản khác.");
        }

        user.setFullName(profileUpdateDto.getFullName());
        user.setEmail(profileUpdateDto.getEmail().trim().toLowerCase());
        user.setAddress(profileUpdateDto.getAddress());
        user.setPhoneNumber(profileUpdateDto.getPhoneNumber());

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User updateShipperProfile(String username, ProfileUpdateDto profileUpdateDto, Long shippingCompanyId) {
        User user = findByUsername(username);
        
        // 1. Cập nhật thông tin cá nhân cơ bản (dùng lại logic cũ)
        User updatedUser = updateUserProfile(username, profileUpdateDto);
        
        // 2. Cập nhật Đơn vị Vận chuyển
        ShippingCompany company = null;
        if (shippingCompanyId != null) {
            company = shippingCompanyRepository.findById(shippingCompanyId)
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy Đơn vị Vận chuyển với ID: " + shippingCompanyId));
            
            if (!company.getIsActive()) {
                 throw new IllegalArgumentException("Đơn vị Vận chuyển này hiện không hoạt động.");
            }
        }
        
        updatedUser.setShippingCompany(company);
        return userRepository.save(updatedUser);
    }

    @Scheduled(fixedRate = 600000)
    @Transactional
    public void cleanupExpiredOtpsAndUnactivatedUsers() {
        LocalDateTime expiryTime = LocalDateTime.now().minusMinutes(10);
        logger.debug("Bắt đầu dọn dẹp OTP hết hạn và user chưa kích hoạt...");

        otpRepository.findByTypeAndExpiresAtBefore("REGISTRATION", expiryTime)
                .forEach(otp -> {
                    User user = otp.getUser();
                    if (user != null && !user.isActivated()) {
                        logger.info("Xóa tài khoản chưa kích hoạt do OTP hết hạn: {}", user.getEmail());
                        otpRepository.delete(otp);
                        userRepository.delete(user);
                    } else if (user != null && user.isActivated()) {
                        logger.debug("Xóa OTP đăng ký đã hết hạn cho user đã kích hoạt: {}", user.getEmail());
                        otpRepository.delete(otp);
                    } else {
                         logger.warn("Xóa OTP đăng ký hết hạn không liên kết với user nào (ID: {})", otp.getId());
                         otpRepository.delete(otp);
                    }
                });

        otpRepository.findByTypeAndExpiresAtBefore("FORGOT", expiryTime)
                 .forEach(otp -> {
                     logger.debug("Xóa OTP quên mật khẩu đã hết hạn cho email: {}", otp.getUser() != null ? otp.getUser().getEmail() : "N/A");
                     otpRepository.delete(otp);
                 });
        logger.debug("Hoàn tất dọn dẹp.");
    }

    @Override
    @Transactional(readOnly = true)
    public User findById(Long id) {
        logger.debug("Finding user by ID: {}", id);
        return userRepository.findById(id).orElse(null);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public User createShipperAccountByAdmin(ShipperCreationDto dto) {
        if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại!");
        }
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email đã được sử dụng bởi tài khoản khác!");
        }

        // 1. Tạo mật khẩu ngẫu nhiên
        String rawPassword = generateRandomPassword();
        
        // 2. Tìm Role SHIPPER
        Role shipperRole = roleRepository.findByName(RoleName.SHIPPER)
                .orElseThrow(() -> new RuntimeException("Vai trò hệ thống SHIPPER không tồn tại."));

        // 3. Tạo User
        User newUser = new User();
        newUser.setUsername(dto.getUsername().trim());
        newUser.setFullName(dto.getFullName().trim());
        newUser.setEmail(dto.getEmail().trim().toLowerCase());
        newUser.setPhoneNumber(dto.getPhoneNumber().trim());
        newUser.setPassword(passwordEncoder.encode(rawPassword));
        newUser.setRole(shipperRole);
        newUser.setActivated(true); // Kích hoạt ngay lập tức
        
        // 4. Lưu User
        User savedUser = userRepository.save(newUser);
        logger.info("Admin created Shipper account for: {}", savedUser.getUsername());

        // 5. Gửi email chứa thông tin đăng nhập
        try {
            emailService.sendShipperCredentials(savedUser.getEmail(), savedUser.getUsername(), rawPassword);
            logger.info("Sent credentials to Shipper email: {}", savedUser.getEmail());
        } catch (Exception e) {
            logger.error("Lỗi gửi email chứa thông tin đăng nhập Shipper cho {}: {}", savedUser.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Tạo tài khoản thành công nhưng LỖI GỬI EMAIL: " + e.getMessage(), e);
        }

        return savedUser;
    }
    
    private String generateRandomPassword() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[8]; 
        random.nextBytes(bytes);
        String base64 = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return base64.substring(0, 10); 
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserRole(Long userId, String newRoleName) {
        logger.warn("ADMIN ACTION: Updating user role for ID {} to {}", userId, newRoleName);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        // 1. Chuyển đổi tên quyền thành Enum
        RoleName roleNameEnum;
        try {
            roleNameEnum = RoleName.valueOf(newRoleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Quyền không hợp lệ: " + newRoleName);
        }

        // 2. Tìm Role entity
        Role newRole = roleRepository.findByName(roleNameEnum)
                .orElseThrow(() -> new EntityNotFoundException("Quyền hệ thống không tồn tại: " + newRoleName));

        // 3. Không cho phép đổi quyền Admin chính
        if (user.getRole().getName() == RoleName.ADMIN && roleNameEnum != RoleName.ADMIN) {
             throw new SecurityException("Không được phép đổi quyền tài khoản ADMIN.");
        }

        // 4. Cập nhật quyền
        user.setRole(newRole);
        userRepository.save(user);
        logger.info("ADMIN ACTION: User ID {} role successfully changed to {}", userId, newRoleName);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleUserStatus(Long userId) {
        logger.warn("ADMIN ACTION: Toggling status for user ID {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        // Không cho phép khóa tài khoản Admin chính
        if (user.getRole().getName() == RoleName.ADMIN) {
             throw new SecurityException("Không được phép khóa/mở khóa tài khoản ADMIN.");
        }

        user.setActivated(!user.isActivated()); // Đảo ngược trạng thái
        userRepository.save(user);
        logger.info("ADMIN ACTION: User ID {} status toggled to {}", userId, user.isActivated() ? "Hoạt động" : "Khóa");
    }
}