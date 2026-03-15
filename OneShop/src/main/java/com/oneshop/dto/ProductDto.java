// src/main/java/com/oneshop/dto/ProductDto.java
package com.oneshop.dto;

import jakarta.validation.Valid; // Thêm import này
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal; // Xóa import này vì giá/kho chuyển sang VariantDto
import java.util.ArrayList; // Thêm import này
import java.util.List;

@Data
public class ProductDto {

    private Long id; // Giữ lại ID khi chỉnh sửa

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 255, message = "Tên sản phẩm quá dài")
    private String productName;

    @NotBlank(message = "Mô tả không được để trống")
    private String productDescription;

    @NotNull(message = "Vui lòng chọn danh mục")
    private Long categoryId;

    // --- THAY ĐỔI VỀ THƯƠNG HIỆU ---
    private Long brandId; // ID của thương hiệu đã chọn (nếu chọn từ dropdown)

    @Size(max = 150, message = "Tên thương hiệu mới quá dài")
    private String newBrandName; // Tên thương hiệu mới (nếu nhập vào textbox)
    // ---------------------------------

    // --- THAY ĐỔI VỀ GIÁ/KHO -> BIẾN THỂ ---
    // Bỏ các trường price, originalPrice, stock
    // private BigDecimal price;
    // private BigDecimal originalPrice;
    // private Integer stock;

    // Thêm danh sách biến thể
    @Valid // Thêm @Valid để kiểm tra validation bên trong VariantDto
    @NotEmpty(message = "Sản phẩm phải có ít nhất một biến thể") // Đảm bảo luôn có biến thể
    private List<VariantDto> variants = new ArrayList<>();
    // ---------------------------------------

    private String productTags;

    private List<String> existingImageUrls; // Giữ nguyên cho ảnh chung của sản phẩm
}