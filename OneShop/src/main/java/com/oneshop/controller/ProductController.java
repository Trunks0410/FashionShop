// src/main/java/com/oneshop/controller/ProductController.java
package com.oneshop.controller;

import com.oneshop.dto.ReviewDTO;
// --- THÊM LẠI CÁC IMPORT CẦN THIẾT ---
import com.oneshop.entity.Brand;
import com.oneshop.entity.Category;
import com.oneshop.entity.Product;
import com.oneshop.entity.ProductImage; // <<< Đã thêm
import com.oneshop.entity.ProductReview;
import com.oneshop.entity.ProductVariant; // <<< Đã thêm
import com.oneshop.entity.ReviewMedia;
import com.oneshop.service.BrandService;
import com.oneshop.service.CategoryService;
import com.oneshop.service.ProductReviewService;
import com.oneshop.service.ProductService;
import com.oneshop.specification.ProductSpecification;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.util.StringUtils; // <<< Đã thêm


import java.math.BigDecimal;
import java.util.ArrayList;   // <<< Đã thêm
import java.util.Collections;
import java.util.HashSet;     // <<< Đã thêm
import java.util.List;
import java.util.Optional;
import java.util.Set;         // <<< Đã thêm
import java.util.stream.Collectors; // <<< Đã thêm
// --- KẾT THÚC PHẦN IMPORT ---

@Controller
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    @Autowired private ProductService productService;
    @Autowired private ProductReviewService reviewService;
    @Autowired private CategoryService categoryService;
    @Autowired private BrandService brandService;

    /**
     * Tìm kiếm sản phẩm (sử dụng method riêng trong service).
     * @deprecated Nên tích hợp vào listProducts với tham số `name`.
     */
   

    /**
     * Hiển thị trang chi tiết sản phẩm.
     * Tạo danh sách ảnh hiển thị riêng biệt.
     */
    @GetMapping("/product/{productId}")
    public String viewProductDetail(@PathVariable Long productId, Model model) {
        logger.info("Viewing product detail page for productId: {}", productId);
        try {
            // Lấy sản phẩm (Service giờ chỉ load, không gộp ảnh)
            Optional<Product> productOpt = productService.findProductById(productId);

            if (productOpt.isEmpty()) {
                 logger.warn("Product not found or not published for productId: {}", productId);
                 throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sản phẩm không tồn tại");
            }

            Product product = productOpt.get();

            // --- TẠO LIST ẢNH HIỂN THỊ RIÊNG ---
            List<String> displayImageUrls = new ArrayList<>();
            Set<String> uniqueUrls = new HashSet<>(); // Dùng Set để đảm bảo URL là duy nhất

            ProductImage primaryImage = null; // Biến tạm lưu ảnh chính

            // 1. Xử lý ảnh chung (product.images)
            if (product.getImages() != null) {
                logger.debug("Processing {} general images for product {}", product.getImages().size(), productId);
                // Tìm và ưu tiên ảnh chính (isPrimary = true)
                for (ProductImage img : product.getImages()) {
                    if (Boolean.TRUE.equals(img.getIsPrimary()) && StringUtils.hasText(img.getImageUrl())) {
                        primaryImage = img;
                        if (uniqueUrls.add(img.getImageUrl())) { // Thêm vào Set và List nếu chưa có
                            displayImageUrls.add(0, img.getImageUrl()); // Thêm vào đầu List
                             logger.trace("Added primary general image: {}", img.getImageUrl());
                        }
                        break; // Đã tìm thấy ảnh chính, thoát vòng lặp ảnh chung
                    }
                }
                // Nếu không tìm thấy ảnh chính (isPrimary=true), thêm các ảnh chung khác (nếu chưa có)
                if (primaryImage == null) {
                     logger.trace("No primary general image found, adding others...");
                     for (ProductImage img : product.getImages()) {
                         if (StringUtils.hasText(img.getImageUrl()) && uniqueUrls.add(img.getImageUrl())) {
                             displayImageUrls.add(img.getImageUrl());
                             logger.trace("Added non-primary general image: {}", img.getImageUrl());
                         }
                     }
                } else {
                    // Nếu đã có ảnh chính, chỉ thêm các ảnh chung phụ (nếu chưa có)
                    logger.trace("Primary general image found, adding secondary...");
                     for (ProductImage img : product.getImages()) {
                         // Bỏ qua ảnh chính đã thêm và ảnh không có URL
                         if (!img.equals(primaryImage) && StringUtils.hasText(img.getImageUrl()) && uniqueUrls.add(img.getImageUrl())) {
                             displayImageUrls.add(img.getImageUrl());
                             logger.trace("Added secondary general image: {}", img.getImageUrl());
                         }
                     }
                }
            } else {
                logger.debug("No general images found for product {}", productId);
            }
            logger.debug("displayImageUrls after processing general images (count: {}): {}", displayImageUrls.size(), displayImageUrls);

            // 2. Xử lý ảnh biến thể (product.variants)
            if (product.getVariants() != null) {
                logger.debug("Processing {} variants for product {}", product.getVariants().size(), productId);
               product.getVariants().forEach(variant -> { // Sử dụng forEach để dễ log hơn
                   String variantImageUrl = variant.getImageUrl();
                    logger.trace("Checking variant '{}' image URL: '{}'", variant.getName(), variantImageUrl); // Log URL đang xét
                   if (StringUtils.hasText(variantImageUrl)) {
                       // Thêm vào Set uniqueUrls, ghi log kết quả trả về của add()
                       boolean added = uniqueUrls.add(variantImageUrl);
                       if (added) {
                           displayImageUrls.add(variantImageUrl);
                            logger.trace("Added unique variant image to display list: {}. Set size now: {}", variantImageUrl, uniqueUrls.size()); // Log khi thêm thành công
                       } else {
                            logger.warn("Skipped duplicate variant image URL: {}", variantImageUrl); // Log khi bị coi là trùng lặp
                       }
                   } else {
                        logger.trace("Variant '{}' has no image URL.", variant.getName()); // Log khi URL rỗng
                   }
               });
           }
            logger.debug("displayImageUrls after processing variant images (count: {}): {}", displayImageUrls.size(), displayImageUrls);

            // 3. Xử lý trường hợp không có ảnh nào cả -> thêm ảnh placeholder
            if (displayImageUrls.isEmpty()) {
                // Sử dụng đường dẫn tương đối bắt đầu bằng /
                displayImageUrls.add("/assets/img/product/no-image.jpg");
                logger.warn("No images found for product {}, adding placeholder.", productId);
            }
            // --- KẾT THÚC TẠO LIST ẢNH ---

            // Lấy reviews và related products (không đổi)
            List<ReviewDTO> reviews = reviewService.getReviewsByProductId(productId);
            List<Product> relatedProducts = productService.findRelatedProducts(product, 6);
            
            
            // Truyền dữ liệu ra Model
            model.addAttribute("product", product); // Vẫn truyền product gốc
            model.addAttribute("displayImages", displayImageUrls); // <<< TRUYỀN LIST MỚI RA VIEW
            model.addAttribute("relatedProducts", relatedProducts);
            model.addAttribute("reviews", reviews);
            

            return "user/product";

        } catch (ResponseStatusException e) {
             throw e; // Ném lại lỗi 404
        } catch (Exception e) {
            logger.error("Error loading product detail page for productId {}: {}", productId, e.getMessage(), e);
            model.addAttribute("errorMessage", "Không thể tải chi tiết sản phẩm.");
            // Xem xét trả về trang lỗi chung thay vì trang sản phẩm rỗng
            return "error/500"; // Giả sử bạn có template error/500.html
        }
    } // <<< ĐẢM BẢO CÓ DẤU NGOẶC NHỌN NÀY

    /**
     * Hiển thị danh sách sản phẩm với bộ lọc, sắp xếp và phân trang.
     * Hỗ trợ cả request thường và AJAX.
     */
    @GetMapping("/products")
    public String listProducts(
    		Model model,
            // Tham số tìm kiếm từ header VÀ form filter
            @RequestParam(name = "name", required = false) String name, // <<< ĐÂY LÀ KEYWORD TÌM KIẾM
            // Filter Parameters
            @RequestParam(name = "categories", required = false) List<Long> categoryIds,
            @RequestParam(name = "brands", required = false) List<Long> brandIds,
            @RequestParam(name = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(name = "rating", required = false) Integer rating, // <<< THÊM RATING VÀO ĐÂY
            // Pagination/Sorting
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size, // Tăng size
            @RequestParam(name = "sort", defaultValue = "productId,desc") String sort, // Mặc định mới nhất
            HttpServletRequest request) {

        logger.debug("Listing/Searching products - Page: {}, Size: {}, Sort: {}, Name: {}, Categories: {}, Brands: {}, Price: {}-{}, Rating: {}",
                     page, size, sort, name, categoryIds, brandIds, minPrice, maxPrice, rating);

        try {
            // 1. Build Specification (bao gồm cả tìm kiếm theo 'name')
            Specification<Product> spec = Specification.where(ProductSpecification.isPublished());
            if (StringUtils.hasText(name)) { // Sử dụng StringUtils.hasText để kiểm tra null và rỗng
                spec = spec.and(ProductSpecification.hasName(name.trim()));
            }
            spec = spec.and(ProductSpecification.hasCategory(categoryIds))
                       .and(ProductSpecification.hasBrand(brandIds))
                       .and(ProductSpecification.priceBetween(minPrice, maxPrice));
                       
            // <<< THÊM ĐIỀU KIỆN LỌC RATING VÀO SPECIFICATION
            if (rating != null) {
                spec = spec.and(ProductSpecification.hasRatingGreaterThanOrEqual(rating));
            }

            // 2. Build Pageable (Đã có validation sort)
            int zeroBasedPage = Math.max(0, page - 1);
            String[] sortParams = sort.split(",");
            Sort.Direction direction = sortParams.length > 1 ? Sort.Direction.fromString(sortParams[1]) : Sort.Direction.DESC;
            String property = sortParams[0];
            List<String> allowedSortProperties = List.of("productId", "price", "salesCount", "rating", "name");
            if (!allowedSortProperties.contains(property)) {
                property = "productId";
                direction = Sort.Direction.DESC;
            }
            Sort sortOrder = Sort.by(direction, property);
            Pageable pageable = PageRequest.of(zeroBasedPage, size, sortOrder);

            // 3. Fetch data (Sử dụng Service trả về Page)
            Page<Product> productPage = productService.findAllPublishedProducts(spec, pageable);

            // 4. Add data to Model
            model.addAttribute("productPage", productPage);
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("brands", brandService.findAll());
            model.addAttribute("searchTerm", name); // Giữ lại giá trị tìm kiếm
            model.addAttribute("selectedCategories", categoryIds);
            model.addAttribute("selectedBrands", brandIds);
            model.addAttribute("minPrice", minPrice);
            model.addAttribute("maxPrice", maxPrice);
            model.addAttribute("rating", rating); // <<< ĐẨY RATING RA VIEW ĐỂ GIỮ TRẠNG THÁI RADIO BUTTON
            model.addAttribute("sort", sort);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", productPage.getTotalPages());
            model.addAttribute("totalItems", productPage.getTotalElements());

            // 5. Check AJAX request (giữ nguyên)
            String requestedWithHeader = request.getHeader("X-Requested-With");
            boolean isAjaxRequest = "XMLHttpRequest".equals(requestedWithHeader);

            if (isAjaxRequest) {
                logger.trace("AJAX request detected, returning product list fragment.");
                // Trả về fragment chứa danh sách sản phẩm và phân trang
                return "user/listProduct :: product_list_fragment";
            } else {
                logger.trace("Normal request detected, returning full product list page.");
                // Trả về trang HTML đầy đủ
                return "user/listProduct"; // Đảm bảo view này tồn tại
            }
        } catch (Exception e) {
             logger.error("Error listing products: {}", e.getMessage(), e);
             model.addAttribute("errorMessage", "Không thể tải danh sách sản phẩm.");
             try {
                 model.addAttribute("categories", categoryService.findAll());
                 model.addAttribute("brands", brandService.findAll());
             } catch (Exception loadEx) {
                 logger.error("Error loading categories/brands on list error page", loadEx);
                 model.addAttribute("categories", Collections.emptyList());
                 model.addAttribute("brands", Collections.emptyList());
             }
             model.addAttribute("productPage", Page.empty(PageRequest.of(Math.max(0, page - 1), size))); // Trả về page rỗng
             // Giữ lại các tham số filter khi lỗi
             model.addAttribute("searchTerm", name);
             model.addAttribute("selectedCategories", categoryIds);
             model.addAttribute("selectedBrands", brandIds);
             model.addAttribute("minPrice", minPrice);
             model.addAttribute("maxPrice", maxPrice);
             model.addAttribute("rating", rating); // <<< ĐẨY RATING VÀO KHI LỖI
             model.addAttribute("sort", sort);
             return "user/listProduct"; // Trả về trang chính với lỗi
        }
    }
} // <<< ĐẢM BẢO CÓ DẤU NGOẶC NHỌN CUỐI CÙNG CỦA CLASS