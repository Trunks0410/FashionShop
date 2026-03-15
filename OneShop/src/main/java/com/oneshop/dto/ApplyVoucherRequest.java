package com.oneshop.dto; // Or your DTO package


public class ApplyVoucherRequest {
    private String code;

    // Add manual Getters and Setters if not using Lombok
    public String getCode() {
        return code;
    }
    public void setCode(String code) {
        this.code = code;
    }
}