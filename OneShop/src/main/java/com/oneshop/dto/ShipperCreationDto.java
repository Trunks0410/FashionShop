package com.oneshop.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern; 
import lombok.Data;

@Data
public class ShipperCreationDto {

    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Pattern(regexp = "^[a-zA-Z0-9]{4,50}$", message = "Tên đăng nhập phải có từ 4-50 ký tự, không dấu, không ký tự đặc biệt")
    private String username;

    @NotBlank(message = "Họ và tên không được để trống")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0|\\+84)[\\d]{9}$", message = "Số điện thoại không hợp lệ")
    private String phoneNumber;
}