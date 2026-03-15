package com.oneshop.controller.admin;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.oneshop.entity.Category;
import com.oneshop.entity.Product;
import com.oneshop.entity.Shop;
import com.oneshop.enums.ProductStatus;
import com.oneshop.enums.ShopStatus;
import com.oneshop.service.CategoryService;
import com.oneshop.service.ProductService;
import com.oneshop.service.ShopService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping("/admin")
public class AdminProductController {
    private static final Logger logger = LoggerFactory.getLogger(AdminProductController.class);

    @Autowired
    private ShopService shopService;

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/shops")
    public String listAllShops(Model model) {
        model.addAttribute("currentPage", "admin-shops");
        logger.debug("Accessing admin shops list page.");
        try {
            List<Shop> shops = shopService.findAll();
            model.addAttribute("shops", shops);
        } catch (Exception e) {
            logger.error("Error fetching shops for admin: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Không thể tải danh sách Shop.");
            model.addAttribute("shops", List.of());
        }
        return "admin/shops";
    }

    @GetMapping("/shops/{shopId}/products")
    public String listShopProducts(@PathVariable Long shopId, @RequestParam(required = false) String productCode,
                                   @RequestParam(required = false) String status, @RequestParam(required = false) Long categoryId,
                                   @RequestParam(required = false) String brand, Model model) {
        model.addAttribute("currentPage", "admin-shops");
        logger.debug("Accessing products list for shopId: {}. Filters - Code: {}, Status: {}, Category: {}, Brand: {}",
                shopId, productCode, status, categoryId, brand);

        String productCodeParam = (productCode != null && productCode.isEmpty()) ? null : productCode;
        String brandParam = (brand != null && brand.isEmpty()) ? null : brand;
        ProductStatus statusEnum = null;
        if (status != null && !status.isEmpty()) {
            try {
                statusEnum = ProductStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid product status filter value received: {}", status);
                model.addAttribute("errorMessage", "Trạng thái lọc không hợp lệ: " + status);
            }
        }
        Long categoryIdParam = categoryId;

        try {
            Shop currentShop = shopService.findById(shopId);
            if (currentShop == null) {
                logger.error("Shop not found with ID: {}", shopId);
                model.addAttribute("errorMessage", "Không tìm thấy Shop với ID: " + shopId);
                return "redirect:/admin/shops";
            }

            List<Product> products = productService.findFilteredProducts(shopId, productCodeParam, statusEnum,
                    categoryIdParam, brandParam);
            List<Category> categories = categoryService.findAll();
            Set<String> brands = productService.findAllUniqueBrands();

            model.addAttribute("products", products);
            model.addAttribute("categories", categories);
            model.addAttribute("brands", brands);
            model.addAttribute("currentShopId", shopId);
            model.addAttribute("currentShopName", currentShop.getName());

            model.addAttribute("selectedProductCode", productCodeParam);
            model.addAttribute("selectedStatusValue", status);
            model.addAttribute("selectedCategoryId", categoryIdParam);
            model.addAttribute("selectedBrand", brandParam);

        } catch (Exception e) {
            logger.error("Error fetching products for shopId {}: {}", shopId, e.getMessage(), e);
            model.addAttribute("errorMessage", "Không thể tải danh sách sản phẩm.");
            model.addAttribute("products", List.of());
            try {
                model.addAttribute("categories", categoryService.findAll());
            } catch (Exception ignored) {}
            try {
                model.addAttribute("brands", productService.findAllUniqueBrands());
            } catch (Exception ignored) {}
            model.addAttribute("currentShopId", shopId);
            try {
                model.addAttribute("currentShopName", shopService.findById(shopId).getName());
            } catch (Exception ignored) {}
        }

        return "admin/shop_products";
    }

    @GetMapping("/products/updateStatus")
    public String updateProductStatus(@RequestParam Long productId, @RequestParam String newStatus,
                                      @RequestParam Long shopId, RedirectAttributes redirectAttributes) {
        logger.info("Attempting to update status for productId: {} to {} for shopId: {}", productId, newStatus, shopId);
        try {
            ProductStatus statusEnum = ProductStatus.valueOf(newStatus.toUpperCase());
            productService.updateStatus(productId, statusEnum);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Cập nhật trạng thái sản phẩm #" + productId + " thành công!");
        } catch (IllegalArgumentException e) {
            logger.error("Invalid status value {} for productId: {}", newStatus, productId);
            redirectAttributes.addFlashAttribute("errorMessage", "Trạng thái cập nhật không hợp lệ: " + newStatus);
        } catch (Exception e) {
            logger.error("Error updating status for productId {}: {}", productId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Lỗi khi cập nhật trạng thái sản phẩm #" + productId + ": " + e.getMessage());
        }
        return "redirect:/admin/shops/" + shopId + "/products";
    }

    @PostMapping("/products/update")
    public String editProduct(@ModelAttribute Product product, @RequestParam Long shopId,
                              RedirectAttributes redirectAttributes) {
        logger.info("Attempting to update product details for productId: {} in shopId: {}", product.getProductId(),
                shopId);
        try {
            productService.updateAdminFields(product);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Cập nhật thông tin sản phẩm #" + product.getProductId() + " thành công!");
        } catch (Exception e) {
            logger.error("Error updating product details for productId {}: {}", product.getProductId(), e.getMessage(),
                    e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Lỗi khi cập nhật sản phẩm #" + product.getProductId() + ": " + e.getMessage());
        }
        return "redirect:/admin/shops/" + shopId + "/products";
    }

    @GetMapping("/products/delete/{id}")
    public String deleteProduct(@PathVariable Long id, @RequestParam Long shopId,
                                RedirectAttributes redirectAttributes) {
        logger.warn("Attempting to delete productId: {} from shopId: {}", id, shopId);
        try {
            productService.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa sản phẩm #" + id + " thành công!");
        } catch (Exception e) {
            logger.error("Error deleting productId {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa sản phẩm #" + id + ": " + e.getMessage());
        }
        return "redirect:/admin/shops/" + shopId + "/products";
    }

    // Xử lý trạng thái Shop
    @GetMapping("/shops/{id}/approve")
    public String approveShop(@PathVariable("id") Long shopId, RedirectAttributes redirectAttributes) {
        logger.info("Attempting to approve shop ID: {}", shopId);
        try {
            shopService.updateShopStatus(shopId, ShopStatus.APPROVED);
            redirectAttributes.addFlashAttribute("successMessage", "Đã duyệt Shop #" + shopId + " thành công!");
        } catch (Exception e) {
            logger.error("Error approving shop ID {}: {}", shopId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi duyệt Shop #" + shopId + ": " + e.getMessage());
        }
        return "redirect:/admin/shops";
    }

    @GetMapping("/shops/{id}/reject")
    public String rejectShop(@PathVariable("id") Long shopId, RedirectAttributes redirectAttributes) {
        logger.warn("Attempting to reject shop ID: {}", shopId);
        try {
            shopService.updateShopStatus(shopId, ShopStatus.REJECTED);
            redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối Shop #" + shopId + ".");
        } catch (Exception e) {
            logger.error("Error rejecting shop ID {}: {}", shopId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi từ chối Shop #" + shopId + ": " + e.getMessage());
        }
        return "redirect:/admin/shops";
    }

    @GetMapping("/shops/{id}/deactivate")
    public String deactivateShop(@PathVariable("id") Long shopId, RedirectAttributes redirectAttributes) {
        logger.warn("Attempting to deactivate shop ID: {}", shopId);
        try {
            shopService.updateShopStatus(shopId, ShopStatus.INACTIVE);
            redirectAttributes.addFlashAttribute("successMessage", "Đã tạm ngưng Shop #" + shopId + ".");
        } catch (Exception e) {
            logger.error("Error deactivating shop ID {}: {}", shopId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi tạm ngưng Shop #" + shopId + ": " + e.getMessage());
        }
        return "redirect:/admin/shops";
    }

    @GetMapping("/shops/{id}/reactivate")
    public String reactivateShop(@PathVariable("id") Long shopId, RedirectAttributes redirectAttributes) {
        logger.info("Attempting to reactivate shop ID: {}", shopId);
        try {
            // Chuyển về APPROVED để kích hoạt trực tiếp (có thể điều chỉnh thành PENDING nếu cần xem xét lại)
            shopService.updateShopStatus(shopId, ShopStatus.APPROVED);
            redirectAttributes.addFlashAttribute("successMessage", "Đã kích hoạt lại Shop #" + shopId + ".");
        } catch (Exception e) {
            logger.error("Error reactivating shop ID {}: {}", shopId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi kích hoạt lại Shop #" + shopId + ": " + e.getMessage());
        }
        return "redirect:/admin/shops";
    }
}