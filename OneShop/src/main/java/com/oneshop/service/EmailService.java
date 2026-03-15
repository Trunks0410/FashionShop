package com.oneshop.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendOtpEmail(String to, String otp, String type) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(type.equals("REGISTRATION") ? "OneShop Registration OTP" : "OneShop Password Reset OTP");
        message.setText("Your OTP is: " + otp + ". It expires in 5 minutes.");
        mailSender.send(message);
    }

    public void sendDeliveryConfirmation(String to, Long orderId) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Đơn hàng của bạn đã được giao thành công");
        message.setText("Kính gửi quý khách,\n\n" +
                        "Đơn hàng #" + orderId + " đã được giao thành công.\n\n" +
                        "Vui lòng đánh giá sản phẩm để giúp chúng tôi cải thiện dịch vụ. " +
                        "Bạn có thể đăng nhập vào tài khoản và truy cập phần đơn hàng để đánh giá.\n\n" +
                        "Cảm ơn quý khách đã mua sắm tại OneShop!");
        mailSender.send(message);
    }
    public void sendShipperCredentials(String to, String username, String password) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Tài khoản Shipper OneShop đã được khởi tạo");
        message.setText("Xin chào " + username + ",\n\n" +
                        "Tài khoản Shipper của bạn trên OneShop đã được khởi tạo thành công.\n\n" +
                        "Thông tin đăng nhập:\n" +
                        "  - Tên đăng nhập: " + username + "\n" +
                        "  - Mật khẩu: " + password + "\n\n" +
                        "Bạn có thể đăng nhập tại trang web của chúng tôi và bắt đầu nhận đơn hàng.\n\n" +
                        "Trân trọng,\n" +
                        "Đội ngũ Quản trị OneShop.");
        mailSender.send(message);
    }
}