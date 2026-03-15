package com.oneshop.entity;

/**
 * Enum định nghĩa các trạng thái của đơn hàng.
 * Tên file: OrderStatus.java
 */
public enum OrderStatus {
    PENDING,           // Chờ thanh toán (đơn hàng vừa tạo, chờ VNPAY/COD)
    CONFIRMED,         // Đã xác nhận (Shop đã tiếp nhận đơn)
    PROCESSING,        // Đang xử lý (Shop đang đóng gói, chuẩn bị hàng) - MỚI
    DELIVERING,        // Đang giao (Đã bàn giao cho đơn vị vận chuyển/shipper)
    DELIVERED,         // Đã giao thành công
    CANCELLED,         // Đã hủy (bởi khách hoặc shop)
    RETURN_REQUESTED,  // Yêu cầu trả hàng (Khách hàng tạo yêu cầu) - MỚI
    RETURNED,          // Đã trả hàng (Shop đã nhận lại hàng)
    REFUNDED           // Đã hoàn tiền (Đã hoàn lại tiền cho khách) - MỚI
}