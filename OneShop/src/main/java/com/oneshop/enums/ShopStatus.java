package com.oneshop.enums;

public enum ShopStatus {
	PENDING, // Đang chờ duyệt (Chờ quản trị viên xem xét)
	APPROVED, // Đã duyệt (Shop đang hoạt động bình thường)
	REJECTED, // Bị từ chối (Không được chấp thuận)
	INACTIVE // Tạm ngưng (Shop bị tạm khóa hoặc ngừng hoạt động)

}
