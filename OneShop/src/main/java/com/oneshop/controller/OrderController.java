package com.oneshop.controller;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com.oneshop.config.VnPayConfig;
import com.oneshop.dto.PlaceOrderRequest;
import com.oneshop.entity.Order;
import com.oneshop.entity.OrderStatus;
import com.oneshop.service.OrderService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private VnPayConfig vnPayConfig;
    
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    
    /**
     * Xác thực kết quả thanh toán từ VNPAY (Callback URL)
     */
    @GetMapping("/vnpay_return")
    public String verifyVnpayPayment(HttpServletRequest request, Model model) {
        
        // 1. Lấy tất cả tham số VNPAY
        Map<String, String> vnpParams = new HashMap<>();
        Enumeration<String> params = request.getParameterNames();
        while (params.hasMoreElements()) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if (fieldValue != null && !fieldName.equals("vnp_SecureHash")) {
                vnpParams.put(fieldName, fieldValue);
            }
        }

        String vnpSecureHash = request.getParameter("vnp_SecureHash");

        // 2. Kiểm tra tính hợp lệ của dữ liệu (Checksum)
        if (vnPayConfig.validateVnPayHash(vnpParams, vnpSecureHash)) {
            
            String vnpResponseCode = request.getParameter("vnp_ResponseCode");
            String vnpTxnRef = request.getParameter("vnp_TxnRef"); // Order ID
            
            if ("00".equals(vnpResponseCode)) {
                // THANH TOÁN THÀNH CÔNG
                try {
                    Long orderId = Long.parseLong(vnpTxnRef);
                  
                    // Lấy thông tin đơn hàng từ Service đã đồng bộ
                    Order order = orderService.getOrderById(orderId);
                    if (order == null || order.getShop() == null) {
                        logger.error("VNPAY success, but Order {} or its Shop is null.", orderId);
                        throw new EntityNotFoundException("Không tìm thấy đơn hàng hoặc Shop liên kết.");
                    }
                    
                    Long shopId = order.getShop().getId();

                    // Cập nhật trạng thái thành CONFIRMED (Đã xác nhận thanh toán)
                    orderService.updateOrderStatus(orderId, OrderStatus.CONFIRMED, shopId); 

                    model.addAttribute("message", "Thanh toán thành công. Đơn hàng #" + orderId + " đã được xác nhận.");
                    model.addAttribute("orderId", orderId); 
                    return "user/order-success"; 
                    
                } catch (Exception e) {
                    logger.error("Error confirming order {} after VNPAY success: {}", vnpTxnRef, e.getMessage());
                    model.addAttribute("message", "Thanh toán thành công nhưng lỗi xử lý đơn hàng. Vui lòng liên hệ hỗ trợ.");
                    return "user/vnpay-error";
                }
            } else {
                // THANH TOÁN THẤT BẠI
                logger.warn("VNPAY payment failed for Order ID: {}. Response code: {}", vnpTxnRef, vnpResponseCode);
                model.addAttribute("message", "Thanh toán thất bại. Mã lỗi VNPAY: " + vnpResponseCode);
                return "user/vnpay-error";
            }
        } else {
            // LỖI CHECKSUM
            logger.error("VNPAY response failed integrity check (Checksum).");
            model.addAttribute("message", "Lỗi bảo mật khi xác thực kết quả thanh toán.");
            return "user/vnpay-error";
        }
    }

    /**
     * Đặt hàng (Checkout/Place Order)
     */
    @PostMapping("/placeOrder")
    @ResponseBody 
    public ResponseEntity<?> placeOrder(
            @RequestBody PlaceOrderRequest orderRequest,
            HttpServletRequest request,
            Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Vui lòng đăng nhập để tiếp tục."));
        }
        String username = principal.getName();

        Order newOrder;
        try {
            // Mapping Controller -> Service -> Domain
            newOrder = orderService.createOrderFromRequest(username, orderRequest);
            if (newOrder == null) {
                 throw new RuntimeException("Không thể tạo đơn hàng do lỗi không xác định.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }

        Map<String, Object> jsonResponse = new HashMap<>();

        // Xử lý theo phương thức thanh toán
        if ("cod".equalsIgnoreCase(orderRequest.getPaymentMethod())) {
            jsonResponse.put("status", "success");
            jsonResponse.put("redirectUrl", "/order/success"); 
            return ResponseEntity.ok(jsonResponse);
            
        } else if ("bank_transfer".equalsIgnoreCase(orderRequest.getPaymentMethod())) {
            if (newOrder.getTotal() == null || newOrder.getTotal().compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Tổng tiền của đơn hàng không hợp lệ."));
            }
            long amount = newOrder.getTotal().multiply(BigDecimal.valueOf(100)).longValue();
            String vnpTxnRef = newOrder.getId().toString();
            String vnpIpAddr = vnPayConfig.getIpAddress(request);
            String vnpOrderInfo = "Thanh toan don hang " + vnpTxnRef;
            
            try {
                // Tạo URL thanh toán VNPAY
                String paymentUrl = vnPayConfig.createPaymentUrl(vnpTxnRef, amount, vnpOrderInfo, vnpIpAddr);
                jsonResponse.put("status", "pending_payment");
                jsonResponse.put("paymentUrl", paymentUrl);
                return ResponseEntity.ok(jsonResponse);
            } catch (Exception e) {
                 logger.error("Error creating VNPAY url: {}", e.getMessage(), e); 
                 return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                      .body(Map.of("message", "Lỗi khi tạo URL thanh toán VNPAY."));
            }
        }

        return ResponseEntity.badRequest().body(Map.of("message", "Phương thức thanh toán không được hỗ trợ."));
    }

    /**
     * Điều hướng hiển thị trang đặt hàng thành công
     */
    @GetMapping("/order/success")
    public String viewOrderSuccessPage() {
        return "user/order-success"; 
    }
}