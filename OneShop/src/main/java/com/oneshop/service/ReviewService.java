package com.oneshop.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; 

import com.oneshop.entity.ProductReview;
import com.oneshop.entity.Order;
import com.oneshop.entity.Product;
import com.oneshop.entity.User;
import com.oneshop.entity.OrderStatus;
import com.oneshop.entity.ReviewMedia; // <<< THÊM IMPORT
import com.oneshop.entity.MediaType; // <<< THÊM IMPORT
import com.oneshop.repository.ProductReviewRepository;
import com.oneshop.repository.OrderRepository;
import com.oneshop.repository.ProductRepository;
import com.oneshop.repository.ReviewMediaRepository; // <<< CẦN REPOSITORY MỚI

import org.springframework.web.multipart.MultipartFile; // <<< THÊM IMPORT
import java.io.IOException; // <<< THÊM IMPORT
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

// Giả định bạn có ReviewService Interface, nhưng vì đây là file Service impl, 
// tôi sẽ coi nó là class triển khai và sửa lại.

@Service
public class ReviewService { // <<< CẦN ĐỔI THÀNH INTERFACE (NẾU CÓ) hoặc là Impl

    // Giả định: Service này là Implementation Class

    @Autowired
    private ProductReviewRepository reviewRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;
    
    // Giả định: Cần FileStorageService để lưu file và ReviewMediaRepository
    @Autowired
    private FileStorageService fileStorageService; 
    
    @Autowired
    private ReviewMediaRepository reviewMediaRepository; 


    /**
     * Lưu đánh giá, bao gồm xử lý file ảnh/video.
     * CẬP NHẬT CHỮ KÝ HÀM.
     */
    @Transactional(rollbackFor = Exception.class) 
    public void saveReview(Long orderId, Long productId, User user, Integer rating, String comment, List<MultipartFile> mediaFiles) {
        
        // 1. Kiểm tra cơ sở
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại!"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại!"));

        if (!order.getUser().getUserId().equals(user.getUserId())) {
            throw new SecurityException("Bạn không có quyền đánh giá đơn hàng này!");
        }

        if (order.getOrderStatus() != OrderStatus.DELIVERED) {
            throw new IllegalStateException("Đơn hàng chưa được giao, không thể đánh giá!");
        }

        boolean hasProduct = order.getOrderDetails().stream()
                .anyMatch(item -> item.getProductVariant() != null && 
                                  item.getProductVariant().getProduct() != null &&
                                  item.getProductVariant().getProduct().getProductId().equals(productId));
        if (!hasProduct) {
            throw new IllegalArgumentException("Sản phẩm không có trong đơn hàng này!");
        }

        if (reviewRepository.findByOrderAndProduct(order, product).isPresent()) {
            throw new IllegalStateException("Sản phẩm này đã được đánh giá trong đơn hàng!");
        }

        // 2. Tạo và lưu Review Entity
        ProductReview review = new ProductReview();
        review.setUser(user);
        review.setProduct(product);
        review.setOrder(order);
        review.setRating(rating);
        review.setComment(comment);
        review.setReviewDate(LocalDateTime.now());

        ProductReview savedReview = reviewRepository.save(review);
        
        // 3. Xử lý và lưu Media Files
        if (mediaFiles != null && !mediaFiles.isEmpty()) {
            for (MultipartFile file : mediaFiles) {
                if (file.isEmpty()) continue;
                
                try {
                    // a. Lưu file vào hệ thống và lấy URL
                    String fileUrl = fileStorageService.storeReviewFile(file);
                    
                    // b. Xác định loại media
                    MediaType mediaType = determineMediaType(file.getContentType());
                    
                    // c. Tạo và lưu ReviewMedia Entity
                    ReviewMedia media = new ReviewMedia();
                    media.setReview(savedReview); // Gán khóa ngoại đến Review vừa lưu
                    media.setMediaUrl(fileUrl);
                    media.setMediaType(mediaType);
                    
                    reviewMediaRepository.save(media);
                    
                } catch (IOException e) {
                    // Cân nhắc xử lý lỗi này: ví dụ: log và tiếp tục, hoặc rollback toàn bộ transaction
                    throw new RuntimeException("Lỗi khi lưu tệp media cho đánh giá.", e);
                }
            }
        }
        
        // (Tùy chọn): Cập nhật lại rating trung bình cho sản phẩm
        // ...
    }
    
    // --- Helper để xác định loại Media ---
    private MediaType determineMediaType(String contentType) {
        if (contentType == null) return MediaType.IMAGE; // Mặc định là ảnh
        if (contentType.startsWith("video/")) {
            return MediaType.VIDEO;
        }
        return MediaType.IMAGE;
    }


    @Transactional(readOnly = true)
    public boolean isProductReviewed(Long orderId, Long productId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        Product product = productRepository.findById(productId).orElse(null);
        if (order == null || product == null) {
            return false;
        }
        return reviewRepository.findByOrderAndProduct(order, product).isPresent();
    }
   
   
}