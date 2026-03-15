package com.oneshop.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import com.oneshop.enums.ProductStatus;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = { "brand", "category", "variants", "images", "shop" })
@ToString(exclude = { "brand", "category", "variants", "images", "shop" })
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    // Thay thế nvarchar(255) bằng length = 255
    @Column(nullable = false, length = 255)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @Column(name = "price", nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(name = "original_price", precision = 19, scale = 2)
    private BigDecimal originalPrice;

    // Thay thế NVARCHAR(MAX) bằng TEXT
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "sales_count", nullable = false)
    private int salesCount = 0;

    @Column(name = "is_published", nullable = false)
    private boolean published = true;

    @Column(nullable = false)
    private Integer stock = 0;

    // Thay thế nvarchar(500) bằng length = 500
    @Column(length = 500)
    private String tags;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<ProductVariant> variants = new HashSet<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<ProductImage> images = new HashSet<>();

    @Transient
    private String primaryImageUrl;

    @Transient
    private Double rating = 0.0;

    @Transient
    private Integer reviewCount = 0;

    @Transient
    private Integer soldCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ProductStatus status = ProductStatus.PENDING;

    public String calculatePrimaryImageUrl() {
        if (images == null || images.isEmpty()) {
            return "/assets/img/product/no-image.jpg";
        }

        ProductImage primaryImage = images.stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .findFirst()
                .orElse(images.iterator().next()); 

        String imageUrl = primaryImage.getImageUrl();
        if (imageUrl != null && !imageUrl.startsWith("/uploads/images/")) {
            return "/uploads/images/" + imageUrl;
        }
        return imageUrl != null ? imageUrl : "/assets/img/product/no-image.jpg";
    }

    public int getDiscountPercentage() {
        if (originalPrice == null || price == null || 
            originalPrice.compareTo(BigDecimal.ZERO) <= 0 ||
            price.compareTo(originalPrice) >= 0) {
            return 0;
        }
        
        BigDecimal discount = originalPrice.subtract(price);
        BigDecimal percentage = discount.multiply(new BigDecimal("100"))
                                      .divide(originalPrice, 0, RoundingMode.HALF_UP);
        return percentage.intValue();
    }

    public String getFormattedDisplayPrice() {
        return formatCurrency(this.price);
    }

    public String getFormattedOriginalPrice() {
        return formatCurrency(this.originalPrice);
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0 đ";
        
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return currencyFormatter.format(amount).replace("₫", "đ");
    }

    public boolean isSelling() {
        return this.published && this.status == ProductStatus.SELLING;
    }

    public boolean isPending() {
        return this.status == ProductStatus.PENDING;
    }

    public boolean isRejected() {
        return this.status == ProductStatus.REJECTED;
    }

    public String getStatusDisplayName() {
        if (this.status == null) return "Không xác định";
        
        switch (this.status) {
            case PENDING: return "Chờ Duyệt";
            case SELLING: return "Đang Bán";
            case REJECTED: return "Bị Từ Chối";
            default: return "Không xác định";
        }
    }

    public void updateStockFromVariants() {
        if (this.variants == null || this.variants.isEmpty()) {
            this.stock = 0;
            return;
        }
        
        this.stock = this.variants.stream()
                .mapToInt(ProductVariant::getStock)
                .sum();
    }

    public void updatePriceFromVariants() {
        if (this.variants == null || this.variants.isEmpty()) {
            this.price = BigDecimal.ZERO;
            this.originalPrice = null;
            return;
        }
        
        this.price = this.variants.stream()
                .map(ProductVariant::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        this.originalPrice = this.variants.stream()
                .filter(v -> v.getPrice().compareTo(this.price) == 0 && v.getOriginalPrice() != null)
                .map(ProductVariant::getOriginalPrice)
                .max(BigDecimal::compareTo)
                .orElse(null);
    }
}