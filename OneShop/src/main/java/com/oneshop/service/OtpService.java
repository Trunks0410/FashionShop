package com.oneshop.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oneshop.entity.Otp;
import com.oneshop.entity.User;
import com.oneshop.repository.OtpRepository;
import com.oneshop.repository.UserRepository;

@Service
public class OtpService {

    @Autowired
    private OtpRepository otpRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    private static final int OTP_LENGTH = 6;
    private static final int EXPIRY_MINUTES = 5;

    public void generateAndSendOtp(String email, String type) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Xóa OTP cũ (nếu có)
        otpRepository.findByUserEmailAndType(email, type).ifPresent(otpRepository::delete);

        String otpCode = generateOtp();

        Otp otp = new Otp();
        otp.setUser(user);
        otp.setOtpCode(otpCode);
        otp.setType(type);
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(EXPIRY_MINUTES));

        otpRepository.save(otp);
        emailService.sendOtpEmail(email, otpCode, type);
    }

    public void verifyOtp(String email, String otpCode, String type) {
        Optional<Otp> otpOpt = otpRepository.findByUserEmailAndType(email, type);

        if (otpOpt.isEmpty()) {
            throw new RuntimeException("OTP not found");
        }

        Otp otp = otpOpt.get();

        if (!otp.getOtpCode().equals(otpCode)) {
            throw new RuntimeException("Invalid OTP");
        }

        if (LocalDateTime.now().isAfter(otp.getExpiresAt())) {
            otpRepository.delete(otp);
            throw new RuntimeException("OTP has expired");
        }

        // Xóa OTP sau khi xác minh thành công
        otpRepository.delete(otp);
    }

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(OTP_LENGTH);
        for (int i = 0; i < OTP_LENGTH; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}