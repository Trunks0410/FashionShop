package com.oneshop.controller;

import com.oneshop.dto.*;
import com.oneshop.entity.*;
import com.oneshop.enums.ShopStatus;
import com.oneshop.repository.UserRepository;
import com.oneshop.service.*;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import com.oneshop.entity.Role.RoleName;
import com.oneshop.repository.RoleRepository;

@Controller
@RequestMapping("/vendor")
@PreAuthorize("hasAuthority('ROLE_VENDOR')")
public class VendorController {

    private static final Logger logger = LoggerFactory.getLogger(VendorController.class);

    @Autowired private ProductService productService;
    @Autowired private CategoryService categoryService;
    @Autowired private OrderService orderService;
    @Autowired private ShopService shopService;
    @Autowired private UserService userService;
    @Autowired private ReportService reportService;
    @Autowired private UserRepository userRepository;
    @Autowired private BrandService brandService;
    @Autowired private ShippingCompanyService shippingCompanyService;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PdfGenerationService pdfGenerationService; 

    

    // --- Helper methods (getAuthenticatedUserEntity, getAndValidateVendorShop, getAuthenticatedShopId) ---
    private User getAuthenticatedUserEntity(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không tìm thấy thông tin người dùng: " + username));
    }

    private Shop getAndValidateVendorShop(Authentication authentication) {
        User user = getAuthenticatedUserEntity(authentication);
        Shop shop = user.getShop();

        if (shop == null) {
            logger.error("User {} is VENDOR but has no associated Shop entity!", user.getUsername());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản của bạn chưa được liên kết với gian hàng nào.");
        }

        if (shop.getStatus() != ShopStatus.APPROVED) {
            logger.warn("Vendor {} attempting to access vendor area, but shop status is {}", user.getUsername(), shop.getStatus());
            throw new ShopNotApprovedException("Shop của bạn chưa được duyệt hoặc đang ở trạng thái không hợp lệ.", shop.getStatus());
        }
        return shop;
    }

    private Long getAuthenticatedShopId(Authentication authentication) {
        return getAndValidateVendorShop(authentication).getId();
    }

    // --- Dashboard ---
    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        String username = authentication.getName();
        logger.info("Accessing vendor dashboard for user: {}", username);
        model.addAttribute("currentPage", "dashboard");

        try {
            Shop shop = getAndValidateVendorShop(authentication);
            Long shopId = shop.getId();

            long newOrderCount = orderService.countNewOrdersByShop(shopId);
            long totalProductCount = productService.countProductsByShop(shopId);
            BigDecimal currentMonthRevenue = orderService.getCurrentMonthRevenueByShop(shopId);

            model.addAttribute("newOrderCount", newOrderCount);
            model.addAttribute("totalProductCount", totalProductCount);
            model.addAttribute("monthlyRevenue", currentMonthRevenue);
            logger.debug("Dashboard data for shopId {}: newOrders={}, totalProducts={}, monthlyRevenue={}",
                shopId, newOrderCount, totalProductCount, currentMonthRevenue);

            return "vendor/dashboard";

        } catch (ShopNotApprovedException e) {
            logger.warn("Redirecting user {} to pending approval page. Status: {}", username, e.getShopStatus());
            model.addAttribute("shopStatus", e.getShopStatus());
            return "vendor/pending_approval";
        } catch (ResponseStatusException e) {
            logger.error("Access forbidden for user {}: {}", username, e.getReason());
            model.addAttribute("errorMessage", e.getReason());
            return "error/403"; // Or appropriate error page
        } catch (Exception e) {
            logger.error("Could not load dashboard data for user {}: {}", username, e.getMessage(), e);
            model.addAttribute("errorMessage", "Không thể tải dữ liệu dashboard. Vui lòng thử lại sau.");
            model.addAttribute("newOrderCount", 0L);
            model.addAttribute("totalProductCount", 0L);
            model.addAttribute("monthlyRevenue", BigDecimal.ZERO);
            return "vendor/dashboard";
        }
    }

    // --- Shop Management ---
    @GetMapping("/shop")
    public String shopManagement(Model model, Authentication authentication) {
        logger.info("Accessing shop management for user: {}", authentication.getName());
        model.addAttribute("currentPage", "shop");
        try {
            User user = getAuthenticatedUserEntity(authentication);
            Shop shop = user.getShop();

            if (shop == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thông tin gian hàng.");
            }

            ShopDto dto = new ShopDto();
            dto.setShopName(shop.getName());
            dto.setShopDescription(shop.getDescription());
            dto.setContactEmail(shop.getContactEmail());
            dto.setContactPhone(shop.getContactPhone());
            model.addAttribute("shopDto", dto);
            model.addAttribute("shop", shop);
            model.addAttribute("shopStatus", shop.getStatus());
            return "vendor/shop_management";
        } catch (ResponseStatusException e) {
            logger.error("Error accessing shop management for user {}: {}", authentication.getName(), e.getReason());
            model.addAttribute("errorMessage", e.getReason());
            model.addAttribute("shopDto", new ShopDto());
            model.addAttribute("shop", null);
            return "vendor/shop_management";
        } catch (Exception e) {
            logger.error("Unexpected error accessing shop management for user {}: {}", authentication.getName(), e.getMessage(), e);
            model.addAttribute("errorMessage", "Không thể tải thông tin gian hàng: " + e.getMessage());
            model.addAttribute("shopDto", new ShopDto());
            model.addAttribute("shop", null);
            return "vendor/shop_management";
        }
    }

    @PostMapping("/shop/update")
    public String updateShop(@Valid @ModelAttribute ShopDto shopDto, BindingResult bindingResult,
            @RequestParam(value = "logoFile", required = false) MultipartFile logoFile,
            @RequestParam(value = "bannerFile", required = false) MultipartFile bannerFile,
            RedirectAttributes redirectAttributes, Authentication authentication, Model model) {
        String username = authentication.getName();
        logger.info("Attempting to update shop for user: {}", username);
        model.addAttribute("currentPage", "shop");
        Shop currentShop = null;

        try {
            User user = getAuthenticatedUserEntity(authentication);
            currentShop = user.getShop();
            if (currentShop == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy gian hàng để cập nhật.");
            }
            // Add shop and status back to model in case of validation errors
            model.addAttribute("shop", currentShop);
            model.addAttribute("shopStatus", currentShop.getStatus());

            if (bindingResult.hasErrors()) {
                logger.warn("Validation errors updating shop for user {}", username);
                return "vendor/shop_management";
            }

            shopService.updateShop(currentShop.getId(), shopDto, logoFile, bannerFile);
            logger.info("Shop updated successfully for user: {}", username);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin gian hàng thành công!");
            return "redirect:/vendor/shop";

        } catch (ResponseStatusException e) {
            logger.error("Error updating shop for user {}: {}", username, e.getReason());
            redirectAttributes.addFlashAttribute("errorMessage", e.getReason());
            return "redirect:/vendor/shop"; // Redirect back to show error
        } catch (Exception e) {
            logger.error("Error updating shop for user {}: {}", username, e.getMessage(), e);
            model.addAttribute("errorMessage", "Cập nhật thất bại: " + e.getMessage());
            model.addAttribute("shopDto", shopDto); // Keep user input
            // model.addAttribute("shop", currentShop); // Already added above
            return "vendor/shop_management";
        }
    }

    // --- Product Management ---
    @GetMapping("/products")
    public String productList(Model model, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size, Authentication authentication) {
        String username = authentication.getName();
        logger.debug("Listing products for user: {}, page: {}, size: {}", username, page, size);
        model.addAttribute("currentPage", "products");
        try {
            Long shopId = getAuthenticatedShopId(authentication);
            Pageable pageable = PageRequest.of(page, size, Sort.by("productId").descending());
            Page<Product> productPage = productService.getProductsByShop(shopId, pageable);
            model.addAttribute("productPage", productPage);
            return "vendor/product_list";
        } catch (ShopNotApprovedException e) {
            logger.warn("Redirecting user {} accessing products to pending page. Status: {}", username, e.getShopStatus());
            model.addAttribute("shopStatus", e.getShopStatus());
            return "vendor/pending_approval";
        } catch (Exception e) {
            logger.error("Error listing products for user {}: {}", username, e.getMessage(), e);
            model.addAttribute("errorMessage", "Không thể tải danh sách sản phẩm.");
            model.addAttribute("productPage", Page.empty(PageRequest.of(page, size)));
            return "vendor/product_list";
        }
    }

    @GetMapping("/products/add")
    public String addProductForm(Model model, Authentication authentication) {
        String username = authentication.getName();
        logger.debug("Showing add product form for user: {}", username);
        model.addAttribute("currentPage", "products");
        try {
            getAndValidateVendorShop(authentication); // Validate shop status

            ProductDto productDto = new ProductDto();
            if (productDto.getVariants().isEmpty()) {
                productDto.getVariants().add(new VariantDto()); // Add one empty variant by default
            }
            model.addAttribute("productDto", productDto);
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("brands", brandService.findAll());
            model.addAttribute("isEditMode", false);
            return "vendor/product_add";
        } catch (ShopNotApprovedException e) {
            logger.warn("Redirecting user {} accessing add product form to pending page. Status: {}", username, e.getShopStatus());
            model.addAttribute("shopStatus", e.getShopStatus());
            return "vendor/pending_approval";
        } catch (Exception e) {
            logger.error("Error preparing add product form for user {}: {}", username, e.getMessage(), e);
            // Use RedirectAttributes if redirecting, otherwise use Model
            model.addAttribute("errorMessage", "Không thể mở trang thêm sản phẩm: " + e.getMessage());
            // It's better to show an error on the product list page than redirecting indefinitely
            return "redirect:/vendor/products";
        }
    }

    @GetMapping("/products/edit/{id}")
    public String editProductForm(@PathVariable("id") Long productId, Model model, Authentication authentication,
            RedirectAttributes redirectAttributes) {
        String username = authentication.getName();
        logger.debug("Showing edit product form for productId: {} for user: {}", productId, username);
        model.addAttribute("currentPage", "products");
        try {
            Long shopId = getAuthenticatedShopId(authentication); // Validate shop status & get ID

            Optional<Product> productOpt = productService.getProductByIdForVendor(productId, shopId);
            if (productOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Sản phẩm không tồn tại hoặc bạn không có quyền sửa.");
                return "redirect:/vendor/products";
            }
            Product product = productOpt.get();
            ProductDto dto = mapProductToDto(product); // Convert entity to DTO
            model.addAttribute("productDto", dto);
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("brands", brandService.findAll());
            model.addAttribute("isEditMode", true);
            return "vendor/product_add";
        } catch (ShopNotApprovedException e) {
            logger.warn("Redirecting user {} accessing edit product form to pending page. Status: {}", username, e.getShopStatus());
            model.addAttribute("shopStatus", e.getShopStatus());
            return "vendor/pending_approval";
        } catch (Exception e) {
            logger.error("Error loading edit product form for productId {}: {}", productId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể tải thông tin sản phẩm để sửa.");
            return "redirect:/vendor/products";
        }
    }

    // mapProductToDto remains the same
    private ProductDto mapProductToDto(Product product) {
        ProductDto dto = new ProductDto();
        dto.setId(product.getProductId());
        dto.setProductName(product.getName());
        dto.setProductDescription(product.getDescription());
        dto.setProductTags(product.getTags());
        if (product.getCategory() != null) dto.setCategoryId(product.getCategory().getId());
        if (product.getBrand() != null) dto.setBrandId(product.getBrand().getBrandId());

        if (product.getImages() != null) {
            dto.setExistingImageUrls(
                    product.getImages().stream()
                        .map(ProductImage::getImageUrl)
                        .filter(StringUtils::hasText)
                        .collect(Collectors.toList()));
        } else {
            dto.setExistingImageUrls(new ArrayList<>());
        }

        if (product.getVariants() != null) {
            dto.setVariants(product.getVariants().stream().map(variant -> {
                VariantDto variantDto = new VariantDto();
                variantDto.setVariantId(variant.getVariantId());
                variantDto.setName(variant.getName());
                variantDto.setSku(variant.getSku());
                variantDto.setPrice(variant.getPrice());
                variantDto.setOriginalPrice(variant.getOriginalPrice());
                variantDto.setStock(variant.getStock());
                variantDto.setExistingImageUrl(variant.getImageUrl());
                return variantDto;
            }).collect(Collectors.toList()));
        } else {
            dto.setVariants(new ArrayList<>());
        }
        if (dto.getVariants().isEmpty()) {
            dto.getVariants().add(new VariantDto());
        }

        return dto;
    }


    // saveProduct remains the same
     @PostMapping("/products/save")
    public String saveProduct(@Valid @ModelAttribute ProductDto productDto,
                              BindingResult bindingResult,
                              @RequestParam(value = "images", required = false) List<MultipartFile> images,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        String username = authentication.getName();
        boolean isEditMode = productDto.getId() != null;
        logger.info("Saving product (edit={}) for user: {}", isEditMode, username);
        model.addAttribute("currentPage", "products");

        Long shopId = null;

        try {
            shopId = getAuthenticatedShopId(authentication);

            // === VALIDATION LOGIC ===
            if (productDto.getBrandId() != null && StringUtils.hasText(productDto.getNewBrandName())) {
                bindingResult.rejectValue("newBrandName", "BrandConflict", "Chỉ chọn thương hiệu có sẵn hoặc nhập tên thương hiệu mới.");
            }
            if (productDto.getVariants() != null && !productDto.getVariants().isEmpty()) {
                 Set<String> uniqueVariantNames = new HashSet<>();
                for (int i = 0; i < productDto.getVariants().size(); i++) {
                    VariantDto variant = productDto.getVariants().get(i);
                    // Giá gốc >= Giá bán
                    if (variant.getOriginalPrice() != null && variant.getPrice() != null &&
                        variant.getOriginalPrice().compareTo(variant.getPrice()) < 0) {
                        bindingResult.rejectValue("variants[" + i + "].originalPrice", "PriceError", "Giá gốc phải lớn hơn hoặc bằng giá bán.");
                    }
                    // Tên variant không rỗng
                    if (!StringUtils.hasText(variant.getName())) {
                        bindingResult.rejectValue("variants[" + i + "].name", "NotBlank", "Tên loại không được để trống.");
                    } else {
                        // Kiểm tra tên trùng (không phân biệt hoa thường)
                        if (!uniqueVariantNames.add(variant.getName().trim().toLowerCase())) {
                             bindingResult.rejectValue("variants[" + i + "].name", "Duplicate", "Tên loại sản phẩm không được trùng nhau.");
                        }
                    }
                }
            } else {
                 bindingResult.reject("variants", "Sản phẩm phải có ít nhất một loại (biến thể).");
            }
            // === KẾT THÚC VALIDATION LOGIC ===


            if (bindingResult.hasErrors()) {
                logger.warn("Validation errors saving product for user {}", username);
                throw new ValidationException("Dữ liệu sản phẩm không hợp lệ.");
            }

            if (!isEditMode) {
                productService.addProduct(productDto, images, shopId);
                redirectAttributes.addFlashAttribute("successMessage", "Thêm sản phẩm thành công! Sản phẩm đang chờ duyệt.");
            } else {
                productService.updateProduct(productDto.getId(), productDto, images, shopId);
                redirectAttributes.addFlashAttribute("successMessage", "Cập nhật sản phẩm thành công! Sản phẩm đang chờ duyệt lại.");
            }
            return "redirect:/vendor/products";

        } catch (ShopNotApprovedException e) {
            logger.warn("Redirecting user {} saving product to pending page. Status: {}", username, e.getShopStatus());
            model.addAttribute("shopStatus", e.getShopStatus()); // Cần truyền status ra view pending
            return "vendor/pending_approval";
        } catch (ValidationException | IllegalArgumentException | EntityNotFoundException e) {
            logger.warn("Validation or business logic error during product save by user {}: {}", username, e.getMessage());
            // --- Xử lý lỗi validation ---
            try {
                model.addAttribute("categories", categoryService.findAll());
                model.addAttribute("brands", brandService.findAll());
            } catch (Exception loadEx) {
                logger.error("Error reloading categories/brands on save error", loadEx);
                model.addAttribute("categories", Collections.emptyList());
                model.addAttribute("brands", Collections.emptyList());
                model.addAttribute("errorMessage", "Lỗi tải lại danh mục/thương hiệu.");
            }
            model.addAttribute("isEditMode", isEditMode);
            // Thêm errorMessage vào model nếu lỗi không phải do validation (vì validation đã có bindingResult)
            if (!(e instanceof ValidationException)) {
                model.addAttribute("errorMessage", "Lỗi: " + e.getMessage());
            }

            // --- Giữ lại ảnh cũ khi lỗi ---
             if (isEditMode && productDto.getId() != null && shopId != null) {
                // Cố gắng load lại ảnh cũ từ DB để hiển thị lại
                try {
                    productService.getProductByIdForVendor(productDto.getId(), shopId)
                        .ifPresent(p -> {
                            ProductDto existingDto = mapProductToDto(p);
                            // Giữ lại ảnh chung cũ
                            productDto.setExistingImageUrls(existingDto.getExistingImageUrls());
                            // Giữ lại ảnh variant cũ tương ứng
                            for(int i = 0; i < productDto.getVariants().size(); i++){
                                VariantDto currentVarDto = productDto.getVariants().get(i);
                                if(currentVarDto.getVariantId() != null){ // Chỉ xử lý variant đã tồn tại
                                    existingDto.getVariants().stream()
                                         .filter(v -> v.getVariantId() != null && v.getVariantId().equals(currentVarDto.getVariantId()))
                                         .findFirst()
                                         .ifPresent(existingVarDto -> {
                                              // Nếu variant này không có file mới upload LÊN, giữ lại ảnh cũ của nó
                                              if (currentVarDto.getVariantImageFile() == null || currentVarDto.getVariantImageFile().isEmpty()) {
                                                  currentVarDto.setExistingImageUrl(existingVarDto.getExistingImageUrl());
                                              }
                                         });
                                }
                            }
                        });
                } catch (Exception imgEx) {
                    logger.error("Error refetching product images/variants on save error", imgEx);
                    // Nếu lỗi load lại, xóa hết ảnh cũ để tránh hiển thị sai
                    productDto.setExistingImageUrls(new ArrayList<>());
                    productDto.getVariants().forEach(v -> v.setExistingImageUrl(null));
                }
            } else { // Trường hợp thêm mới bị lỗi, không có ảnh cũ
                productDto.setExistingImageUrls(new ArrayList<>());
                productDto.getVariants().forEach(v -> v.setExistingImageUrl(null));
            }
            // --- Kết thúc giữ lại ảnh cũ ---

            return "vendor/product_add"; // Quay lại form add/edit
        } catch (Exception e) {
            logger.error("Error saving product for user {}: {}", username, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Lưu sản phẩm thất bại: " + e.getMessage());
            return isEditMode ? ("redirect:/vendor/products/edit/" + productDto.getId()) : "redirect:/vendor/products/add";
        }
    }


    // deleteProduct remains the same
    @PostMapping("/products/delete/{id}")
    public String deleteProduct(@PathVariable("id") Long productId, Authentication authentication,
            RedirectAttributes redirectAttributes) {
        String username = authentication.getName();
        logger.warn("Attempting to delete product {} by user {}", productId, username);
        try {
            Long shopId = getAuthenticatedShopId(authentication);
            productService.deleteProduct(productId, shopId);
            logger.info("Product {} deleted successfully by user {}", productId, username);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa sản phẩm thành công!");
        } catch (ShopNotApprovedException e) {
            logger.warn("User {} cannot delete product, shop not approved. Status: {}", username, e.getShopStatus());
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa sản phẩm khi shop chưa được duyệt.");
            return "redirect:/vendor/dashboard"; // Hoặc trang pending
        } catch (Exception e) {
            logger.error("Error deleting product {} by user {}: {}", productId, username, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Xóa sản phẩm thất bại: " + e.getMessage());
        }
        return "redirect:/vendor/products";
    }


    // --- Order Management ---
    // orderList remains the same
    @GetMapping("/orders")
    public String orderList(Model model, @RequestParam(name = "status", required = false) String statusString,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        String username = authentication.getName();
        logger.debug("Listing orders for user: {}, status: {}, page: {}, size: {}", username, statusString, page, size);
        model.addAttribute("currentPage", "orders");
        try {
            Long shopId = getAuthenticatedShopId(authentication);

            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Optional<OrderStatus> orderStatusOptional = Optional.empty();
            if (statusString != null && !statusString.trim().isEmpty()) {
                try {
                    orderStatusOptional = Optional.of(OrderStatus.valueOf(statusString.trim().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    model.addAttribute("errorMessage", "Trạng thái đơn hàng không hợp lệ: " + statusString);
                    // Optional rỗng là mặc định, không cần làm gì thêm
                }
            }
            Page<Order> orderPage = orderService.getOrdersByShop(shopId, orderStatusOptional, pageable);
            model.addAttribute("orderPage", orderPage);
            model.addAttribute("currentStatus", statusString); // Truyền status string gốc để active tab
            return "vendor/order_list";
        } catch (ShopNotApprovedException e) {
            logger.warn("Redirecting user {} accessing orders to pending page. Status: {}", username, e.getShopStatus());
            model.addAttribute("shopStatus", e.getShopStatus());
            return "vendor/pending_approval";
        } catch (Exception e) {
            logger.error("Error listing orders for user {}: {}", username, e.getMessage(), e);
            model.addAttribute("errorMessage", "Không thể tải danh sách đơn hàng.");
            model.addAttribute("orderPage", Page.empty(PageRequest.of(page, size)));
            model.addAttribute("currentStatus", statusString); // Vẫn truyền status để giữ tab (nếu có)
            return "vendor/order_list";
        }
    }


    // *** SỬA LỖI TRONG orderDetails ***
    @GetMapping("/orders/details/{id}")
    public String orderDetails(@PathVariable("id") Long orderId, Model model, Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        String username = authentication.getName();
        logger.debug("Viewing order details for orderId: {} by user: {}", orderId, username);
        model.addAttribute("currentPage", "orders");
        try {
            Long shopId = getAuthenticatedShopId(authentication);
            Order order = orderService.getOrderDetails(orderId, shopId);
            model.addAttribute("order", order);

            // Lấy danh sách đơn vị vận chuyển hoạt động
            List<ShippingCompany> activeShippingCompanies = shippingCompanyService.findActiveCompanies();
            model.addAttribute("shippingCompanies", activeShippingCompanies);

            // Lấy danh sách shipper khả dụng
            Role shipperRole = roleRepository.findByName(RoleName.SHIPPER).orElse(null);
            List<User> availableShippers = Collections.emptyList();

            if (shipperRole != null) {
                // *** SỬA LỖI Ở ĐÂY: Dùng stream() filter() thay vì phương thức không tồn tại ***
                availableShippers = userRepository.findAll().stream()
                    .filter(u -> u.getRole() != null && u.getRole().getId().equals(shipperRole.getId()) && u.isActivated())
                    .collect(Collectors.toList());
                // *** KẾT THÚC SỬA LỖI ***
            }
            model.addAttribute("availableShippers", availableShippers);

            return "vendor/order_details";
        } catch (ShopNotApprovedException e) {
            logger.warn("Redirecting user {} accessing order details to pending page. Status: {}", username, e.getShopStatus());
            model.addAttribute("shopStatus", e.getShopStatus());
            return "vendor/pending_approval";
        } catch (SecurityException | EntityNotFoundException e) {
            logger.warn("Access denied or order not found for user {} on orderId {}: {}", username, orderId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/vendor/orders";
        } catch (Exception e) {
            logger.error("Error viewing order details for orderId {} by user {}: {}", orderId, username, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi khi xem chi tiết đơn hàng.");
            return "redirect:/vendor/orders";
        }
    }
    
    
    // *** KẾT THÚC SỬA LỖI ***

    // updateOrderStatus remains the same
    @PostMapping("/orders/update-status")
    public String updateOrderStatus(@RequestParam("orderId") Long orderId,
            @RequestParam("status") String statusString,
            Authentication authentication, RedirectAttributes redirectAttributes) {
        String username = authentication.getName();
        logger.info("User {} attempting to update orderId {} to status {}", username, orderId, statusString);
        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(statusString.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid status value received: {}", statusString);
            redirectAttributes.addFlashAttribute("errorMessage", "Trạng thái cập nhật không hợp lệ.");
            return "redirect:/vendor/orders/details/" + orderId;
        }

        try {
            Long shopId = getAuthenticatedShopId(authentication);
            orderService.updateOrderStatus(orderId, newStatus, shopId);
            logger.info("Order {} status updated to {} by user {}", orderId, newStatus, username);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái đơn hàng thành công!");
        } catch (ShopNotApprovedException e) {
            logger.warn("User {} cannot update order status, shop not approved. Status: {}", username, e.getShopStatus());
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể cập nhật trạng thái khi shop chưa được duyệt.");
            return "redirect:/vendor/dashboard"; // Hoặc trang pending
        } catch (SecurityException e) {
            logger.warn("Access denied for user {} trying to update orderId {}: {}", username, orderId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền cập nhật đơn hàng này.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            logger.warn("Failed to update status for orderId {} by user {}: {}", orderId, username, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Cập nhật trạng thái thất bại: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error updating status for orderId {} by user {}: {}", orderId, username, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi khi cập nhật trạng thái.");
        }
        // Redirect về trang chi tiết đơn hàng
        return "redirect:/vendor/orders/details/" + orderId;
    }


    // --- Revenue Management ---
    @GetMapping("/revenue")
    public String revenueManagement(Model model, Authentication authentication) {
    	model.addAttribute("currentPage", "revenue");
        String username = authentication.getName();
        logger.info("Accessing revenue management for user: {}", username);
        try {
            Long shopId = getAuthenticatedShopId(authentication);

            // Dữ liệu cũ
            BigDecimal totalRevenue = orderService.getTotalRevenueByShop(shopId);
            BigDecimal currentMonthRevenue = orderService.getCurrentMonthRevenueByShop(shopId);
            long totalDeliveredOrders = orderService.countDeliveredOrdersByShop(shopId);
            Map<String, BigDecimal> monthlyData = orderService.getMonthlyRevenueData(shopId, 6); // Biểu đồ đường

            Map<String, BigDecimal> chartLabelsAndData = new LinkedHashMap<>();
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("MM/yyyy");
            monthlyData.forEach((key, value) -> {
                try {
                    String formattedLabel = YearMonth.parse(key, inputFormatter).format(outputFormatter);
                    chartLabelsAndData.put(formattedLabel, value);
                } catch (Exception e) {
                     logger.error("Error formatting month label: {}", key, e);
                    chartLabelsAndData.put(key, value); // Giữ key gốc nếu lỗi
                }
            });

            model.addAttribute("totalRevenue", totalRevenue);
            model.addAttribute("currentMonthRevenue", currentMonthRevenue);
            model.addAttribute("totalDeliveredOrders", totalDeliveredOrders);
            model.addAttribute("chartLabels", chartLabelsAndData.keySet()); // Nhãn biểu đồ đường
            model.addAttribute("chartData", chartLabelsAndData.values()); // Dữ liệu biểu đồ đường

            // Lấy dữ liệu mới cho biểu đồ cột và tròn
            Map<String, BigDecimal> topProductRevenue = orderService.getTopProductRevenueByShop(shopId, 5); // Lấy top 5 SP
            model.addAttribute("topProductLabels", new ArrayList<>(topProductRevenue.keySet()));
            model.addAttribute("topProductData", new ArrayList<>(topProductRevenue.values()));

            Map<String, BigDecimal> categoryRevenue = orderService.getCategoryRevenueDistributionByShop(shopId);
            model.addAttribute("categoryLabels", new ArrayList<>(categoryRevenue.keySet()));
            model.addAttribute("categoryData", new ArrayList<>(categoryRevenue.values()));

            logger.debug("Revenue data loaded successfully for shopId {}", shopId);
            return "vendor/revenue";

        } catch (ShopNotApprovedException e) {
            model.addAttribute("shopStatus", e.getShopStatus());
            return "vendor/pending_approval";
        } catch (Exception e) {
            logger.error("Error loading revenue data for user {}: {}", username, e.getMessage(), e);
            model.addAttribute("errorMessage", "Không thể tải dữ liệu doanh thu: " + e.getMessage());
            // Cung cấp giá trị mặc định cho tất cả thuộc tính
            model.addAttribute("totalRevenue", BigDecimal.ZERO);
            model.addAttribute("currentMonthRevenue", BigDecimal.ZERO);
            model.addAttribute("totalDeliveredOrders", 0L);
            model.addAttribute("chartLabels", List.of());
            model.addAttribute("chartData", List.of());
            model.addAttribute("topProductLabels", List.of());
            model.addAttribute("topProductData", List.of());
            model.addAttribute("categoryLabels", List.of());
            model.addAttribute("categoryData", List.of());
            return "vendor/revenue";
        }
    }


    // --- Profile Management ---
    // showProfileForm and updateProfile remain the same
     @GetMapping("/profile")
    public String showProfileForm(Model model, Authentication authentication) {
        model.addAttribute("currentPage", "profile");
        User currentUser = getAuthenticatedUserEntity(authentication);
        ProfileUpdateDto dto = new ProfileUpdateDto();
        dto.setFullName(currentUser.getFullName());
        dto.setEmail(currentUser.getEmail());
        dto.setAddress(currentUser.getAddress()); // Địa chỉ chung của User
        dto.setPhoneNumber(currentUser.getPhoneNumber());
        model.addAttribute("profileUpdateDto", dto);
        return "vendor/profile"; // View của Vendor
    }

    @PostMapping("/profile/update")
    public String updateProfile(@Valid @ModelAttribute ProfileUpdateDto profileUpdateDto, BindingResult bindingResult,
            Authentication authentication, RedirectAttributes redirectAttributes, Model model) {
        model.addAttribute("currentPage", "profile");
        if (bindingResult.hasErrors()) {
            return "vendor/profile"; // Quay lại form với lỗi
        }
        String username = authentication.getName();
        try {
            userService.updateUserProfile(username, profileUpdateDto);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hồ sơ thành công!");
            return "redirect:/vendor/profile";
        } catch (IllegalArgumentException e) { // Bắt lỗi email trùng
            bindingResult.rejectValue("email", "Unique", e.getMessage());
            model.addAttribute("profileUpdateDto", profileUpdateDto); // Giữ lại dữ liệu đã nhập
            return "vendor/profile"; // Quay lại form
        } catch (Exception e) {
            logger.error("Error updating profile for user {}: {}", username, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Cập nhật hồ sơ thất bại: " + e.getMessage());
            return "redirect:/vendor/profile";
        }
    }


    // --- Category Management ---
    @GetMapping("/categories")
    public String categoryManagement(Model model, Authentication authentication) {
        String username = authentication.getName();
        model.addAttribute("currentPage", "categories"); // <<< THÊM DÒNG NÀY
        try {
            getAndValidateVendorShop(authentication); // Kiểm tra shop hợp lệ
            List<Category> categories = categoryService.findAll(); // Lấy tất cả danh mục
            model.addAttribute("categories", categories);
            model.addAttribute("newCategory", new Category()); // DTO cho form thêm mới
            return "vendor/category_management";
        } catch (ShopNotApprovedException e) {
            model.addAttribute("shopStatus", e.getShopStatus());
            return "vendor/pending_approval";
        } catch (Exception e) {
            logger.error("Error fetching categories for user {}: {}", username, e.getMessage(), e);
            model.addAttribute("errorMessage", "Không thể tải danh mục.");
            model.addAttribute("categories", Collections.emptyList());
            model.addAttribute("newCategory", new Category());
            return "vendor/category_management";
        }
    }

    @PostMapping("/categories/add")
    public String addCategory(@Valid @ModelAttribute("newCategory") Category newCategory, BindingResult bindingResult,
            RedirectAttributes redirectAttributes, Model model, Authentication authentication) {
        String username = authentication.getName();
        model.addAttribute("currentPage", "categories");
        try {
            getAndValidateVendorShop(authentication);

            if (bindingResult.hasErrors()) {
                // Nếu có lỗi validation cơ bản (như @NotBlank)
                 logger.warn("Validation errors adding category: {}", bindingResult.getAllErrors());
                 throw new ValidationException("Dữ liệu danh mục không hợp lệ."); // Ném lỗi để xử lý chung bên dưới
            }
            // Logic lưu category (Service sẽ kiểm tra trùng tên)
            categoryService.saveCategory(newCategory);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm danh mục '" + newCategory.getName() + "' thành công!");
            return "redirect:/vendor/categories";

        } catch (ShopNotApprovedException e) {
             model.addAttribute("shopStatus", e.getShopStatus());
            return "vendor/pending_approval";
        } catch (ValidationException | IllegalArgumentException e) { // Bắt lỗi validation và lỗi trùng tên từ service
            logger.warn("Error adding category for user {}: {}", username, e.getMessage());
            try {
                 model.addAttribute("categories", categoryService.findAll()); // Tải lại danh sách
            } catch (Exception catEx) {
                 logger.error("Error reloading categories on add error", catEx);
                 model.addAttribute("categories", Collections.emptyList());
            }
            // Nếu không phải lỗi ValidationException (nghĩa là lỗi từ service như trùng tên), thêm lỗi vào bindingResult
            if (!(e instanceof ValidationException)) {
                bindingResult.rejectValue("name", "Unique", e.getMessage());
            }
            // model tự động chứa newCategory và bindingResult
            return "vendor/category_management"; // Quay lại form với lỗi
        } catch (Exception e) {
            logger.error("Unexpected error adding category for user {}: {}", username, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Thêm danh mục thất bại: " + e.getMessage());
            return "redirect:/vendor/categories";
        }
    }

    @PostMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable("id") Long id, RedirectAttributes redirectAttributes, Authentication authentication) {
        String username = authentication.getName();
        logger.warn("Attempting to delete category ID: {} by user {}", id, username);
        try {
            getAndValidateVendorShop(authentication);
            categoryService.deleteCategory(id); // Service sẽ kiểm tra sản phẩm
            logger.info("Category {} deleted by user {}.", id, username);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa danh mục thành công!");
        } catch (ShopNotApprovedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa danh mục khi shop chưa được duyệt.");
            return "redirect:/vendor/dashboard"; // Hoặc trang pending
        } catch (RuntimeException e) { // Bắt lỗi từ service (ví dụ: đang dùng bởi sản phẩm)
            logger.error("Error deleting category {} by user {}: {}", id, username, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Xóa danh mục thất bại: " + e.getMessage());
        }
        return "redirect:/vendor/categories";
    }


    // --- Brand Management ---
    @GetMapping("/brands")
    public String brandManagement(Model model, Authentication authentication) {
        String username = authentication.getName();
        model.addAttribute("currentPage", "brands"); 
        try {
            getAndValidateVendorShop(authentication); // Kiểm tra shop
            List<Brand> brands = brandService.findAll(); // Lấy tất cả brands
            model.addAttribute("brands", brands);
            model.addAttribute("newBrand", new Brand()); 
            return "vendor/brand_management";
        } catch (ShopNotApprovedException e) {
            model.addAttribute("shopStatus", e.getShopStatus());
            return "vendor/pending_approval";
        } catch (Exception e) {
            logger.error("Error fetching brands for user {}: {}", username, e.getMessage(), e);
            model.addAttribute("errorMessage", "Không thể tải danh sách thương hiệu.");
            model.addAttribute("brands", Collections.emptyList());
            model.addAttribute("newBrand", new Brand());
            return "vendor/brand_management";
        }
    }

    @PostMapping("/brands/add")
    public String addBrand(@Valid @ModelAttribute("newBrand") Brand newBrand, BindingResult bindingResult,
                           RedirectAttributes redirectAttributes, Model model, Authentication authentication) {
        String username = authentication.getName();
        model.addAttribute("currentPage", "brands");
        try {
            getAndValidateVendorShop(authentication);

            if (bindingResult.hasErrors()) {
                 logger.warn("Validation errors adding brand: {}", bindingResult.getAllErrors());
                 throw new ValidationException("Dữ liệu thương hiệu không hợp lệ.");
            }
            // Logic lưu brand (Service sẽ kiểm tra trùng tên)
            brandService.saveBrand(newBrand);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm thương hiệu '" + newBrand.getName() + "' thành công!");
            return "redirect:/vendor/brands";

        } catch (ShopNotApprovedException e) {
            model.addAttribute("shopStatus", e.getShopStatus());
            return "vendor/pending_approval";
        } catch (ValidationException | IllegalArgumentException e) {
            logger.warn("Error adding brand for user {}: {}", username, e.getMessage());
            try {
                 model.addAttribute("brands", brandService.findAll()); // Tải lại danh sách
            } catch (Exception brandEx) {
                 logger.error("Error reloading brands on add error", brandEx);
                 model.addAttribute("brands", Collections.emptyList());
            }
            // Nếu không phải lỗi ValidationException (nghĩa là lỗi từ service), thêm lỗi vào bindingResult
            if (!(e instanceof ValidationException)) {
                bindingResult.rejectValue("name", "Unique", e.getMessage());
            }
            return "vendor/brand_management"; // Quay lại form với lỗi
        } catch (Exception e) {
            logger.error("Unexpected error adding brand for user {}: {}", username, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Thêm thương hiệu thất bại: " + e.getMessage());
            return "redirect:/vendor/brands";
        }
    }

    @PostMapping("/brands/delete/{id}")
    public String deleteBrand(@PathVariable("id") Long id, RedirectAttributes redirectAttributes, Authentication authentication) {
        String username = authentication.getName();
        logger.warn("Attempting to delete brand ID: {} by user {}", id, username);
        try {
            getAndValidateVendorShop(authentication);
            brandService.deleteBrand(id); // Service sẽ kiểm tra sản phẩm (nếu có logic đó)
            logger.info("Brand {} deleted by user {}.", id, username);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa thương hiệu thành công!");
        } catch (ShopNotApprovedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa thương hiệu khi shop chưa được duyệt.");
            return "redirect:/vendor/dashboard"; // Hoặc trang pending
        } catch (RuntimeException e) { // Bắt lỗi từ service
            logger.error("Error deleting brand {} by user {}: {}", id, username, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Xóa thương hiệu thất bại: " + e.getMessage());
        }
        return "redirect:/vendor/brands";
    }


    // --- Reports ---
    // downloadSalesReport and sendErrorResponse remain the same
     @GetMapping("/reports/sales/download")
    public void downloadSalesReport(Authentication authentication, HttpServletResponse response) {
        String username = authentication.getName();
        logger.info("User {} requesting sales report download.", username);
        Long shopId = null;
        try {
            shopId = getAuthenticatedShopId(authentication); // Lấy shop ID đã xác thực

            Workbook workbook = reportService.generateSalesReport(shopId); // Gọi service tạo báo cáo
            String fileName = "BaoCaoBanHang_Shop" + shopId + "_" + java.time.LocalDate.now() + ".xlsx";

            // Thiết lập response headers
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

            // Ghi workbook vào output stream của response
            workbook.write(response.getOutputStream());
            workbook.close(); // Đóng workbook
            response.flushBuffer(); // Đảm bảo dữ liệu được gửi đi
            logger.info("Sales report sent successfully for shopId: {}", shopId);

        } catch (ShopNotApprovedException e) {
            logger.warn("User {} cannot download report, shop not approved. Status: {}", username, e.getShopStatus());
            sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "Không thể tải báo cáo khi shop chưa được duyệt.");
        } catch (IOException e) {
            // Lỗi khi ghi file vào response output stream
            logger.error("IOException writing sales report for shopId {}: {}", (shopId != null ? shopId : "null"), e.getMessage());
            // Không cần gửi lỗi nữa vì response có thể đã bị commit một phần
        } catch (Exception e) {
            // Các lỗi khác (ví dụ: lỗi tạo workbook trong service)
            logger.error("Error generating or writing sales report for shopId {}: {}", (shopId != null ? shopId : "null"), e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Lỗi khi tạo báo cáo: " + e.getMessage());
        }
    }

    // Helper gửi lỗi về client khi download report thất bại
    private void sendErrorResponse(HttpServletResponse response, int status, String message) {
        try {
            if (!response.isCommitted()) { // Chỉ gửi nếu response chưa bị commit
                response.setStatus(status);
                response.setContentType("text/plain; charset=utf-8"); // Đặt kiểu content là text
                response.getWriter().write(message); // Ghi thông báo lỗi
                response.getWriter().flush();
            }
        } catch (IOException ioEx) {
            logger.error("Error writing error response after report generation failed: {}", ioEx.getMessage());
        }
    }


    // --- AJAX Endpoints ---
    // updateShippingDetailsAjax and assignShipperAjax remain the same
    @PostMapping("/orders/update-shipping")
    @ResponseBody // Đánh dấu trả về JSON
    public ResponseEntity<?> updateShippingDetailsAjax(
            @RequestParam("orderId") Long orderId,
            @RequestParam("shippingCompanyId") Long shippingCompanyId,
            Authentication authentication) {

        String username = authentication.getName();
        logger.info("AJAX request from user {} to update shipping for order {} with company {}", username, orderId, shippingCompanyId);

        try {
            Long shopId = getAuthenticatedShopId(authentication); // Xác thực shop
            // Gọi service để cập nhật
            Order updatedOrder = orderService.updateShippingDetails(orderId, shopId, shippingCompanyId);

            // Chuẩn bị dữ liệu trả về thành công
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cập nhật đơn vị vận chuyển thành công!");
            response.put("newShippingCost", updatedOrder.getShippingCost()); // Phí VC mới
            response.put("newTotal", updatedOrder.getTotal()); // Tổng tiền mới
            response.put("selectedCompanyId", updatedOrder.getShippingCompany() != null ? updatedOrder.getShippingCompany().getShippingId() : null); // ID công ty đã chọn

            return ResponseEntity.ok(response); // Trả về 200 OK với JSON

        } catch (ShopNotApprovedException e) {
             return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "Shop chưa được duyệt."));
        } catch (SecurityException e) { // Bắt lỗi không có quyền
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "Bạn không có quyền cập nhật đơn hàng này."));
        } catch (EntityNotFoundException | IllegalArgumentException | IllegalStateException e) { // Lỗi nghiệp vụ
            logger.warn("Validation/State error during shipping update for order {}: {}", orderId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) { // Lỗi hệ thống
            logger.error("Unexpected error updating shipping for order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "Lỗi hệ thống khi cập nhật vận chuyển."));
        }
    }

    @PostMapping("/orders/assign-shipper")
    @ResponseBody // Đánh dấu trả về JSON
    public ResponseEntity<?> assignShipperAjax(
            @RequestParam("orderId") Long orderId,
            @RequestParam("shipperId") Long shipperId,
            Authentication authentication) {

        String username = authentication.getName();
        logger.info("AJAX request from user {} to assign Shipper ID: {} to order {}", username, shipperId, orderId);

        try {
            Long shopId = getAuthenticatedShopId(authentication); // Xác thực shop
            // Gọi service để gán shipper và cập nhật trạng thái
            Order updatedOrder = orderService.assignShipper(orderId, shopId, shipperId);

            // Chuẩn bị dữ liệu trả về thành công
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Gán Shipper và chuyển trạng thái sang Đang giao thành công!");
            response.put("newStatus", updatedOrder.getOrderStatus().name()); // Trạng thái mới (DELIVERING)
            // Lấy tên shipper (ưu tiên fullName)
            response.put("shipperName", updatedOrder.getShipper().getFullName() != null ? updatedOrder.getShipper().getFullName() : updatedOrder.getShipper().getUsername());
             // Trả về cả phí ship và tổng tiền mới (có thể không đổi nhưng để JS cập nhật cho chắc)
            response.put("newShippingCost", updatedOrder.getShippingCost());
            response.put("newTotal", updatedOrder.getTotal());


            return ResponseEntity.ok(response); // Trả về 200 OK

        } catch (ShopNotApprovedException e) {
             return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "Shop chưa được duyệt."));
        } catch (SecurityException e) { // Không có quyền
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "Bạn không có quyền cập nhật đơn hàng này."));
        } catch (EntityNotFoundException | IllegalArgumentException | IllegalStateException e) { // Lỗi nghiệp vụ
            logger.warn("Validation/State error during shipper assignment for order {}: {}", orderId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) { // Lỗi hệ thống
            logger.error("Unexpected error assigning shipper for order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "Lỗi hệ thống khi gán Shipper."));
        }
    }


    // --- Inner Exception classes remain the same ---
    public static class ShopNotApprovedException extends RuntimeException {
        private final ShopStatus shopStatus;
        public ShopNotApprovedException(String message, ShopStatus status) { super(message); this.shopStatus = status; }
        public ShopStatus getShopStatus() { return shopStatus; }
    }
    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) { super(message); }
    }
    
    // pdf
    @GetMapping("/orders/{id}/packing-slip")
    public ResponseEntity<byte[]> downloadPackingSlip(@PathVariable("id") Long orderId, 
                                                    Authentication authentication) {
        String username = authentication.getName();
        logger.info("User {} requesting packing slip for orderId: {}", username, orderId);
        try {
            // 1. Xác thực vendor và shop
            Long shopId = getAuthenticatedShopId(authentication); 
            
            // 2. Lấy đơn hàng (dùng hàm đã có sẵn check quyền sở hữu)
            Order order = orderService.getOrderDetails(orderId, shopId); 
            
            // 3. Tạo PDF
            byte[] pdfContents = pdfGenerationService.generatePackingSlip(order);

            // 4. Chuẩn bị Response Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String filename = "phieu-xuat-hang-" + order.getId() + ".pdf";
            headers.setContentDispositionFormData(filename, filename);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfContents);
                    
        } catch (ShopNotApprovedException e) {
            logger.warn("User {} cannot download packing slip, shop not approved. Status: {}", username, e.getShopStatus());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (SecurityException | EntityNotFoundException e) {
            logger.warn("Access denied or order not found for user {} on orderId {}: {}", username, orderId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            logger.error("Error generating packing slip for orderId {} by user {}: {}", orderId, username, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Endpoint để tạo và tải về Phiếu Gửi Hàng (Shipping Label)
     * URL: /vendor/orders/{id}/shipping-label
     */
    @GetMapping("/orders/{id}/shipping-label")
    public ResponseEntity<byte[]> downloadShippingLabel(@PathVariable("id") Long orderId, 
                                                      Authentication authentication) {
        String username = authentication.getName();
        logger.info("User {} requesting shipping label for orderId: {}", username, orderId);
        try {
            // 1. Xác thực vendor và shop
            Long shopId = getAuthenticatedShopId(authentication); 
            
            // 2. Lấy đơn hàng (dùng hàm đã có sẵn check quyền sở hữu)
            Order order = orderService.getOrderDetails(orderId, shopId); 
            
            // 3. Tạo PDF
            byte[] pdfContents = pdfGenerationService.generateShippingLabel(order);

            // 4. Chuẩn bị Response Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String filename = "phieu-gui-hang-" + order.getId() + ".pdf";
            headers.setContentDispositionFormData(filename, filename);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfContents);

        } catch (ShopNotApprovedException e) {
            logger.warn("User {} cannot download shipping label, shop not approved. Status: {}", username, e.getShopStatus());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (SecurityException | EntityNotFoundException e) {
            logger.warn("Access denied or order not found for user {} on orderId {}: {}", username, orderId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            logger.error("Error generating shipping label for orderId {} by user {}: {}", orderId, username, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
}