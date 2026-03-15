package com.oneshop.controller;

import com.oneshop.entity.Order;               
import com.oneshop.entity.OrderDetail;          
import com.oneshop.entity.Product;             
import com.oneshop.entity.User;                
import com.oneshop.service.OrderService;         
import com.oneshop.service.ReviewService;        

import org.hibernate.Hibernate;              
import org.slf4j.Logger;                     
import org.slf4j.LoggerFactory;            
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;    
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart; // <<< THÊM: Import RequestPart
import org.springframework.web.multipart.MultipartFile;     // <<< THÊM: Import MultipartFile
import org.springframework.web.server.ResponseStatusException; 
import org.springframework.web.servlet.mvc.support.RedirectAttributes; 

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ReviewController {

    private static final Logger logger = LoggerFactory.getLogger(ReviewController.class); 

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private OrderService orderService;

    /**
     * Hiển thị form đánh giá cho một đơn hàng đã giao.
     */
    @GetMapping("/user/orders/{orderId}/review")
    public String showReviewForm(@PathVariable Long orderId,
                                 @AuthenticationPrincipal User user, 
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        logger.info("Showing review form for orderId: {} for user: {}", orderId, user.getUsername());
        try {
            Order order = orderService.getOrderDetail(orderId, user.getUsername()); 

            if (order.getOrderStatus() != com.oneshop.entity.OrderStatus.DELIVERED) {
                 logger.warn("Attempt to review order {} which is not DELIVERED (status: {}) by user {}", orderId, order.getOrderStatus(), user.getUsername());
                 redirectAttributes.addFlashAttribute("errorMessage", "Chỉ có thể đánh giá đơn hàng đã giao thành công.");
                 return "redirect:/user/orders"; 
            }

            Hibernate.initialize(order.getOrderDetails());
            List<OrderDetail> items = new ArrayList<>(order.getOrderDetails()); 

            List<Map<String, Object>> productReviewsInfo = new ArrayList<>();
            for (OrderDetail item : items) {
                if (item.getProductVariant() != null && item.getProductVariant().getProduct() != null) {
                    Map<String, Object> reviewInfo = new HashMap<>();
                    Product product = item.getProductVariant().getProduct();
                    reviewInfo.put("product", product); 
                    boolean reviewed = reviewService.isProductReviewed(orderId, product.getProductId()); 
                    reviewInfo.put("reviewed", reviewed);
                    productReviewsInfo.add(reviewInfo);
                } else {
                     logger.warn("Order detail item in order {} has null ProductVariant or Product.", orderId);
                }
            }

            model.addAttribute("order", order);
            model.addAttribute("productReviews", productReviewsInfo); 
            return "user/review-form"; 

        } catch (ResponseStatusException e) {
             logger.warn("User {} attempted to access review form for order {} without permission or order not found.", user.getUsername(), orderId);
             redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy đơn hàng hoặc bạn không có quyền truy cập.");
             return "redirect:/user/orders";
        } catch (Exception e) {
            logger.error("Error showing review form for orderId {}: {}", orderId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi tải trang đánh giá.");
            return "redirect:/user/orders";
        }
    }

    /**
     * Xử lý việc gửi đánh giá cho một sản phẩm trong đơn hàng, bao gồm tải lên ảnh/video.
     */
    @PostMapping("/user/orders/{orderId}/review/{productId}")
    public String submitReview(@PathVariable Long orderId,
                              @PathVariable Long productId,
                              @RequestParam Integer rating, 
                              @RequestParam(required = false, defaultValue = "") String comment, 
                              @RequestParam(value = "mediaFiles", required = false) List<MultipartFile> mediaFiles, // <<< THÊM: Nhận file
                              @AuthenticationPrincipal User user, 
                              RedirectAttributes redirectAttributes) { 
        logger.info("Submitting review for orderId: {}, productId: {} by user: {}", orderId, productId, user.getUsername());

        // Validation cơ bản
        if (rating == null || rating < 1 || rating > 5) {
             redirectAttributes.addFlashAttribute("errorMessage_" + productId, "Vui lòng chọn số sao đánh giá (1-5).");
             return "redirect:/user/orders/" + orderId + "/review";
        }
        
        // Lọc bỏ file rỗng (nếu người dùng không chọn file nào, hoặc chọn rồi xóa)
        List<MultipartFile> validMediaFiles = mediaFiles != null ? mediaFiles.stream()
            .filter(file -> file != null && !file.isEmpty())
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll) : new ArrayList<>();
            
        // Có thể thêm validation giới hạn số lượng file ở đây nếu cần

        try {
            // Cập nhật Service Call để bao gồm danh sách files
            reviewService.saveReview(orderId, productId, user, rating, comment, validMediaFiles); // <<< CẬP NHẬT SERVICE CALL
            logger.info("Review submitted successfully for productId: {} in orderId: {}. Files attached: {}", 
                        productId, orderId, validMediaFiles.size());
            redirectAttributes.addFlashAttribute("successMessage", "Đánh giá sản phẩm thành công!");
            
            return "redirect:/user/orders/" + orderId + "/review";
        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            logger.warn("Review submission failed for productId: {} in orderId: {}: {}", productId, orderId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage_" + productId, e.getMessage());
            return "redirect:/user/orders/" + orderId + "/review";
        } catch (Exception e) {
            logger.error("Unexpected error submitting review for productId: {} in orderId: {}: {}", productId, orderId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi khi gửi đánh giá.");
            return "redirect:/user/orders/" + orderId + "/review";
        }
    }
}