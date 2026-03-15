package com.oneshop.service;

import com.oneshop.dto.PlaceOrderRequest;
import com.oneshop.entity.Order;
import com.oneshop.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface OrderService {

    // --- Vendor ---
    Page<Order> getOrdersByShop(Long shopId, Optional<OrderStatus> status, Pageable pageable);
    Order getOrderDetails(Long orderId, Long shopId);
    Order updateOrderStatus(Long orderId, OrderStatus newStatus, Long shopId);
    long countNewOrdersByShop(Long shopId);

    BigDecimal getTotalRevenueByShop(Long shopId);
    BigDecimal getCurrentMonthRevenueByShop(Long shopId);
    long countDeliveredOrdersByShop(Long shopId);
    Order updateShippingDetails(Long orderId, Long shopId, Long shippingCompanyId);
    Map<String, BigDecimal> getMonthlyRevenueData(Long shopId, int months);
    
    Order assignShipper(Long orderId, Long shopId, Long shipperId); 

    // --- User (Khách hàng) - Refactored theo SSD ---
    List<Order> getOrders(String username); 
    Order getOrderDetail(Long orderId, String username); 
    Order createOrderFromRequest(String username, PlaceOrderRequest orderRequest);
    void cancelOrder(Long orderId, String username);
    void confirmOrderReceived(Long orderId, String username); 
    void createReturnRequest(Long orderId, String username, String reason, String description); 
    
    Order getOrderById(Long orderId); 

    // --- Shipper ---
    List<Order> getAssignedOrders(Long shipperId);
    Map<String, Long> getOrderStats(Long shipperId);
    void deliverOrder(Long orderId, Long shipperId);
    void updateShipperOrderStatus(Long orderId, Long shipperId, OrderStatus newStatus); 
    
    Map<String, BigDecimal> getTopProductRevenueByShop(Long shopId, int limit);
    Map<String, BigDecimal> getCategoryRevenueDistributionByShop(Long shopId);
}