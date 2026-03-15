// src/main/java/com/oneshop/entity/Order.java
package com.oneshop.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter 
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"orderDetails", "user", "shop", "shipper", "promotion", "shippingCompany"})
@ToString(exclude = {"orderDetails", "user", "shop", "shipper", "promotion", "shippingCompany"})
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; 

    // Đã thay thế nvarchar(255) bằng length = 255
    @Column(name = "recipient_name", nullable = false, length = 255)
    private String recipientName;

    @Column(name = "shipping_phone", nullable = false, length = 20)
    private String shippingPhone;

    // Đã thay thế nvarchar(500) bằng length = 500
    @Column(name = "shipping_address", nullable = false, length = 500)
    private String shippingAddress; 

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length=50)
    private OrderStatus orderStatus; 

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(precision = 19, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "shipping_cost", precision = 19, scale = 2)
    private BigDecimal shippingCost;
    
    @Column(name = "discount_amount", precision = 19, scale = 2)
    private BigDecimal discountAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id") 
    private Promotion promotion; 
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_company_id") 
    private ShippingCompany shippingCompany;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal total;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipper_id") 
    private User shipper; 

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderDetail> orderDetails = new ArrayList<>(); 

    public void recalculateTotal() {
        BigDecimal calculatedTotal = BigDecimal.ZERO;
        if (this.subtotal != null) {
            calculatedTotal = calculatedTotal.add(this.subtotal);
        }
        if (this.shippingCost != null) {
            calculatedTotal = calculatedTotal.add(this.shippingCost);
        }
        if (this.discountAmount != null) {
            calculatedTotal = calculatedTotal.subtract(this.discountAmount);
        }
        this.total = calculatedTotal.max(BigDecimal.ZERO);
    }
}