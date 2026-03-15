package com.oneshop.enums;

public enum ProductStatus {
//Sản phẩm phải được admin duyệt mỗi khi thêm mới
	PENDING("Chờ Duyệt"), 

// Sản phẩm đã được Admin phê duyệt và đang được bán/hiển thị công khai.
    SELLING("Đang Bán"), 

//Sản phẩm đã bị Admin từ chối do vi phạm quy tắc, không được hiển thị công khai.
    REJECTED("Từ Chối"); 

    private final String displayName;
    ProductStatus(String displayName) {
        this.displayName = displayName;
    }
    public String getDisplayName() {
        return displayName;
    }
}
