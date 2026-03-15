// src/main/java/com/oneshop/dto/VariantDto.java
package com.oneshop.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import org.springframework.web.multipart.MultipartFile; // <<< THÊM IMPORT NÀY

@Data
public class VariantDto {
    private Long variantId; // Cần khi cập nhật

    @NotBlank(message = "Tên biến thể không được để trống")
    @Size(max = 100, message = "Tên biến thể quá dài")
    private String name; // Ví dụ: "Màu Đỏ", "Size L"

    @Size(max = 50, message = "SKU quá dài")
    private String sku; // Mã SKU (tùy chọn)

    @NotNull(message = "Giá biến thể không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá biến thể phải lớn hơn 0")
    private BigDecimal price; // Giá của biến thể này

    @DecimalMin(value = "0.0", inclusive = true, message = "Giá gốc phải lớn hơn hoặc bằng 0")
    private BigDecimal originalPrice; // Giá gốc (tùy chọn)

    @NotNull(message = "Tồn kho biến thể không được để trống")
    @Min(value = 0, message = "Tồn kho không được âm")
    private Integer stock; // Tồn kho của biến thể này

    // --- THÊM CÁC TRƯỜNG CHO ẢNH BIẾN THỂ ---
    private MultipartFile variantImageFile; // Để nhận file tải lên từ form
    private String existingImageUrl; // Để hiển thị ảnh hiện có khi edit
    // -----------------------------------------
}