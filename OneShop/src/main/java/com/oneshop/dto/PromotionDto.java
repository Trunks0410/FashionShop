package com.oneshop.dto;
import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank; // Thêm NotBlank
import jakarta.validation.constraints.Size;    // Thêm Size
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PromotionDto {

    private Long id; // <<< THÊM TRƯỜNG ID ĐỂ BIẾT LÀ SỬA HAY THÊM

    @NotBlank(message = "Tên chiến dịch không được để trống") // Thêm validation
    @Size(max=255, message = "Tên chiến dịch quá dài")
    private String campaignName;

    @NotBlank(message = "Mã giảm giá không được để trống") // Thêm validation
    @Size(max=100, message = "Mã giảm giá quá dài")
    @NotNull // Đảm bảo không null khi validate
    private String discountCode;

    @NotNull(message = "Vui lòng chọn loại khuyến mãi")
    private Long promotionTypeId;

    // Giá trị giảm (có thể null nếu là FREE_SHIPPING)
    private BigDecimal discountValue;

    @NotNull(message = "Ngày bắt đầu không được để trống") // Thêm validation
    private LocalDate startDate;

    @NotNull(message = "Ngày kết thúc không được để trống") // Thêm validation
    private LocalDate endDate;

    // TODO: Thêm validation để đảm bảo endDate >= startDate (có thể dùng custom validator)
}