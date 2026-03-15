// src/main/java/com/oneshop/service/ProductService.java
package com.oneshop.service; // Đảm bảo package đúng

import com.oneshop.dto.ProductDto;
import com.oneshop.entity.Brand;
import com.oneshop.entity.Category;
import com.oneshop.entity.Product;
import com.oneshop.enums.ProductStatus;

import jakarta.transaction.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal; // Import BigDecimal
import java.util.List;
import java.util.Optional;
import java.util.Set;

// Đây là Interface, chỉ khai báo phương thức
public interface ProductService {
	
	String calculatePrimaryImageUrl(Product product);

    // --- Chức năng cho Vendor ---
    /**
     * Lấy danh sách sản phẩm của một Shop (phân trang).
     * @param shopId ID của Shop.
     * @param pageable Thông tin phân trang.
     * @return Page<Product>.
     */
    Page<Product> getProductsByShop(Long shopId, Pageable pageable);

    /**
     * Lấy thông tin chi tiết một sản phẩm cho Vendor (kiểm tra quyền sở hữu).
     * @param productId ID sản phẩm.
     * @param shopId ID của Shop sở hữu.
     * @return Optional<Product>.
     */
    Optional<Product> getProductByIdForVendor(Long productId, Long shopId); // Đổi tên để phân biệt với hàm public

    /**
     * Thêm sản phẩm mới cho Shop.
     * @param productDto DTO chứa thông tin.
     * @param images Danh sách file ảnh.
     * @param shopId ID của Shop.
     * @return Product đã lưu.
     */
    Product addProduct(ProductDto productDto, List<MultipartFile> images, Long shopId);

    /**
     * Cập nhật thông tin sản phẩm.
     * @param productId ID sản phẩm cần cập nhật.
     * @param productDto DTO chứa thông tin mới.
     * @param newImages Danh sách file ảnh mới (có thể rỗng).
     * @param shopId ID của Shop sở hữu (để kiểm tra quyền).
     * @return Product đã cập nhật.
     */
    Product updateProduct(Long productId, ProductDto productDto, List<MultipartFile> newImages, Long shopId); // Sửa: Thêm shopId

    /**
     * Xóa sản phẩm.
     * @param productId ID sản phẩm cần xóa.
     * @param shopId ID của Shop sở hữu (để kiểm tra quyền).
     */
    void deleteProduct(Long productId, Long shopId); // Sửa: Thêm shopId

    /**
     * Đếm số lượng sản phẩm của Shop.
     * @param shopId ID Shop.
     * @return Số lượng sản phẩm.
     */
    long countProductsByShop(Long shopId);

    // --- Chức năng cho User (Public) ---
    /**
     * Lấy tất cả danh mục.
     * @return List<Category>.
     */
    List<Category> getAllCategories();

    /**
     * Lấy tất cả thương hiệu.
     * @return List<Brand>.
     */
    List<Brand> getAllBrands();

    /**
     * Lấy danh sách sản phẩm bán chạy nhất (đã published).
     * @param limit Số lượng tối đa cần lấy.
     * @return List<Product>.
     */
    List<Product> findBestSellingProducts(int limit);

    /**
     * Lấy danh sách sản phẩm mới nhất (đã published), có phân trang.
     * @param pageable Thông tin phân trang (trang, kích thước, sắp xếp).
     * @return Page<Product>.
     */
    Page<Product> findNewestProducts(Pageable pageable);

    /**
     * Lấy danh sách sản phẩm có giá tốt/giảm giá nhiều nhất (đã published).
     * @param limit Số lượng tối đa cần lấy.
     * @return List<Product>.
     */
    List<Product> findBestPriceProducts(int limit);

    /**
     * Tìm sản phẩm theo ID (chỉ sản phẩm đã published).
     * @param productId ID sản phẩm.
     * @return Optional<Product>.
     */
    Optional<Product> findProductById(Long productId); // Sửa: Trả về Optional

    /**
     * Tìm sản phẩm liên quan (cùng category, khác ID, đã published).
     * @param product Sản phẩm hiện tại.
     * @param limit Số lượng tối đa cần lấy.
     * @return List<Product>.
     */
    List<Product> findRelatedProducts(Product product, int limit); // Sửa: Thêm limit
    
//	// ADMIN
//	private final CategoryService categoryService;
//
//	@Autowired
//	public ProductService(ProductRepository productRepository, CategoryService categoryService) {
//		this.productRepository = productRepository;
//		this.categoryService = categoryService;
//	}
//
//	@Transactional(readOnly = true)
//	public List<Product> findFilteredProducts(Long shopId, String productCode, ProductStatus status, Long categoryId,
//			String brand) {
//		String codeParam = (productCode != null && !productCode.isEmpty()) ? productCode : null;
//		String brandParam = (brand != null && !brand.isEmpty()) ? brand : null;
//		return productRepository.findFilteredProducts(shopId, codeParam, status, categoryId, brandParam);
//	}
//
//	@Transactional(readOnly = true)
//	public Set<String> findAllUniqueBrands() {
//		return productRepository.findAllUniqueBrands();
//	}
//
////Xử lý việc duyệt/từ chối (cập nhật trạng thái).
//
//	@Transactional
//	public void updateStatus(Long productId, ProductStatus newStatus) {
//		productRepository.findById(productId).ifPresent(product -> {
//			product.setStatus(newStatus);
//		});
//	}
//
////Xử lý chỉnh sửa thông tin Admin được phép thay đổi.
//	@Transactional
//	public void updateAdminFields(Product productDetails) {
//		productRepository.findById(productDetails.getProductId()).ifPresent(product -> {
//			product.setName(productDetails.getName());
//			product.setDescription(productDetails.getDescription());
//			if (productDetails.getCategory() != null && productDetails.getCategory().getCategoryId() != null) {
//				Long newCategoryId = productDetails.getCategory().getCategoryId();
//				categoryService.findById(newCategoryId).ifPresent(product::setCategory);
//			}
//		});
//	}
//
//	@Transactional
//	public void deleteById(Long id) {
//		productRepository.deleteById(id);
//	}
//
//	@Transactional
//	public int updateCategoryForProducts(Long oldCategoryId, Long newCategoryId) {
//		return productRepository.updateCategoryByCategoryId(oldCategoryId, newCategoryId);
//	}
//  //====================================================================================
    /**
     * Tìm kiếm và lọc tất cả sản phẩm đã published, có phân trang.
     * @param spec Specification chứa điều kiện lọc.
     * @param pageable Thông tin phân trang và sắp xếp.
     * @return Page<Product>.
     */
    Page<Product> findAllPublishedProducts(Specification<Product> spec, Pageable pageable); // Sửa tên: findAllPublishedProducts

    /**
     * Tìm kiếm và lọc sản phẩm public (không phân trang).
     * @param name Tên sản phẩm (có thể null).
     * @param categoryId ID danh mục (có thể null).
     * @param brandId ID thương hiệu (có thể null).
     * @param minPrice Giá tối thiểu (có thể null).
     * @param maxPrice Giá tối đa (có thể null).
     * @return List<Product>.
     */
    List<Product> searchAndFilterPublic(String name, Long categoryId, Long brandId, BigDecimal minPrice, BigDecimal maxPrice); 
    
   
 
    
    void updateProductStockAndPriceFromVariants(Product product);
  //main 
    
    
 // ===>>> CÁC PHƯƠNG THỨC MỚI ĐƯỢC HỢP NHẤT TỪ PHẦN CLASS CŨ <<<===

   // List<Product> findFeaturedProducts();

    List<Product> findFilteredProducts(Long shopId, String productCode, ProductStatus status, Long categoryId,
            String brand);

    /**
     * Lấy danh sách tất cả các thương hiệu duy nhất
     */
    Set<String> findAllUniqueBrands();

    /**
     * Cập nhật trạng thái duyệt/từ chối cho sản phẩm (ADMIN)
     */
    void updateStatus(Long productId, ProductStatus newStatus);

    /**
     * Cập nhật các trường mà ADMIN được phép thay đổi
     */
    void updateAdminFields(Product productDetails);

    /**
     * Xóa sản phẩm bằng ID (ADMIN)
     */
    void deleteById(Long id);

    /**
     * Cập nhật lại Category cho các sản phẩm khi Category cũ bị xóa/hợp nhất (ADMIN)
     */
    int updateCategoryForProducts(Long oldCategoryId, Long newCategoryId);

	long countProductsByCategory(Long categoryId);

    // ===>>> KẾT THÚC HỢP NHẤT <<<===
}