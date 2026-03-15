package com.oneshop.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.oneshop.entity.Order;
import com.oneshop.entity.User;
import com.oneshop.service.OrderService;
import com.oneshop.service.ReviewService;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Controller
@RequestMapping("/user/orders")
public class UserOrdersController {

    @Autowired
    private OrderService orderService;
    
    @Autowired
    private ReviewService reviewService;
    private static final Logger log = LoggerFactory.getLogger(UserOrdersController.class);

    @GetMapping
    public String getOrders(@AuthenticationPrincipal User user, Model model) {
        String username = user.getUsername(); 
    	List<Order> userOrders = orderService.getOrders(username); 

        model.addAttribute("orders", userOrders);
        return "user/orders"; 
    }
    
    @PostMapping("/{id}/cancel")
    @ResponseBody
    public ResponseEntity<?> cancelOrder(@PathVariable Long id, @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Vui lòng đăng nhập."));
        }
        try {
            orderService.cancelOrder(id, user.getUsername());
            return ResponseEntity.ok(Map.of("message", "Đơn hàng #" + id + " đã được hủy thành công."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/confirm-received")
    @ResponseBody
    public ResponseEntity<?> confirmOrderReceived(@PathVariable Long id, @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Vui lòng đăng nhập."));
        }
        try {
            orderService.confirmOrderReceived(id, user.getUsername());
            return ResponseEntity.ok(Map.of("message", "Đã xác nhận nhận hàng thành công."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/return")
    @ResponseBody
    public ResponseEntity<?> createReturnRequest(
            @PathVariable Long id, 
            @RequestParam String reason, 
            @RequestParam(required = false) String description,
            @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Vui lòng đăng nhập."));
        }
        try {
            orderService.createReturnRequest(id, user.getUsername(), reason, description);
            return ResponseEntity.ok(Map.of("message", "Yêu cầu trả hàng đã được gửi thành công."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/{id}/details")
    public String getOrderDetail(@PathVariable Long id, @AuthenticationPrincipal User user, Model model) {
        String username = user.getUsername();
        Order order = orderService.getOrderDetail(id, username); 
        model.addAttribute("order", order);
        return "user/order-details-fragment :: content";
    }

    @GetMapping("/{orderId}/reviewed/{productId}")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> checkReviewed(
            @PathVariable Long orderId,
            @PathVariable Long productId,
            @AuthenticationPrincipal User user) {
        try {
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }
            Order order = orderService.getOrderDetail(orderId, user.getUsername());
            if (order == null) {
                return ResponseEntity.ok(Map.of("reviewed", false));
            }
            boolean reviewed = reviewService.isProductReviewed(orderId, productId);
            return ResponseEntity.ok(Map.of("reviewed", reviewed));
        } catch (Exception e) {
            log.error("Lỗi khi kiểm tra review cho orderId={} và productId={}: {}", orderId, productId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}