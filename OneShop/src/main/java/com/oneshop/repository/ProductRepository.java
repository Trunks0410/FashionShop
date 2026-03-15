package com.oneshop.repository;

import com.oneshop.entity.Product;
import com.oneshop.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    // --- Cho Vendor ---
    Page<Product> findByShopId(Long shopId, Pageable pageable);
    Page<Product> findByShopIdAndNameContainingIgnoreCase(Long shopId, String name, Pageable pageable);
    long countByShopId(Long shopId);
    long countByCategoryId(Long categoryId);
    List<Product> findByCategory_Id(Long categoryId);

    // --- Cho User (Public) - ĐÃ CẬP NHẬT ---

    /**
     * Tìm sản phẩm đã publish (có phân trang).
     * Giữ lại hàm cũ để tương thích ngược
     */
    Page<Product> findByPublishedTrue(Pageable pageable);

    /**
     * Tìm sản phẩm đã publish VÀ đã được duyệt (SELLING) (có phân trang).
     */
    Page<Product> findByPublishedTrueAndStatus(boolean published, ProductStatus status, Pageable pageable);

    /**
     * Tìm sản phẩm theo ID VÀ đã publish.
     * Giữ lại hàm cũ để tương thích ngược
     */
    Optional<Product> findByProductIdAndPublishedTrue(Long productId);

    /**
     * Tìm sản phẩm theo ID VÀ đã publish VÀ đã được duyệt (SELLING).
     */
    Optional<Product> findByProductIdAndPublishedTrueAndStatus(Long productId, ProductStatus status);

    /**
     * Tìm sản phẩm cùng category, khác ID, đã publish (có phân trang).
     * Giữ lại hàm cũ để tương thích ngược
     */
    List<Product> findByCategory_IdAndProductIdNotAndPublishedTrue(Long categoryId, Long productId, Pageable pageable);

    /**
     * Tìm sản phẩm cùng category, khác ID, đã publish VÀ đã được duyệt (SELLING).
     */
    List<Product> findByCategory_IdAndProductIdNotAndPublishedTrueAndStatus(Long categoryId, Long productId, ProductStatus status, Pageable pageable);

    /**
     * Lấy top N sản phẩm bán chạy nhất (đã publish).
     * Giữ lại hàm cũ để tương thích ngược
     */
    @Query("SELECT p FROM Product p WHERE p.published = true ORDER BY p.salesCount DESC")
    Page<Product> findTopSellingPublished(Pageable pageable);

    /**
     * Lấy top N sản phẩm bán chạy nhất (đã publish và SELLING).
     */
    @Query("SELECT p FROM Product p WHERE p.published = true AND p.status = :status ORDER BY p.salesCount DESC")
    Page<Product> findTopSellingPublishedAndSelling(@Param("status") ProductStatus status, Pageable pageable);

    /**
     * Lấy top N sản phẩm mới nhất (đã publish).
     * Giữ lại hàm cũ để tương thích ngược
     */
    @Query("SELECT p FROM Product p WHERE p.published = true ORDER BY p.productId DESC")
    Page<Product> findTopNewestPublished(Pageable pageable);

    /**
     * Lấy top N sản phẩm mới nhất (đã publish và SELLING).
     */
    @Query("SELECT p FROM Product p WHERE p.published = true AND p.status = :status ORDER BY p.productId DESC")
    Page<Product> findTopNewestPublishedAndSelling(@Param("status") ProductStatus status, Pageable pageable);

    /**
     * Lấy top N sản phẩm giảm giá nhiều nhất (đã publish).
     * Giữ lại hàm cũ để tương thích ngược
     */
    @Query("SELECT p FROM Product p WHERE p.published = true AND p.originalPrice IS NOT NULL AND p.originalPrice > p.price " +
           "ORDER BY ((p.originalPrice - p.price) / p.originalPrice) DESC")
    Page<Product> findTopDiscountedPublished(Pageable pageable);

    /**
     * Lấy top N sản phẩm giảm giá nhiều nhất (đã publish và SELLING).
     */
    @Query("SELECT p FROM Product p WHERE p.published = true AND p.status = :status AND p.originalPrice IS NOT NULL AND p.originalPrice > p.price " +
           "ORDER BY ((p.originalPrice - p.price) / p.originalPrice) DESC")
    Page<Product> findTopDiscountedPublishedAndSelling(@Param("status") ProductStatus status, Pageable pageable);

    /**
     * Tìm kiếm và lọc sản phẩm public (published=true).
     * Giữ lại hàm cũ để tương thích ngược
     */
    @Query("SELECT p FROM Product p WHERE p.published = true " +
           "AND (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
           "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
           "AND (:brandId IS NULL OR p.brand.brandId = :brandId) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice)")
    List<Product> searchAndFilterPublic(@Param("name") String name, 
                                      @Param("categoryId") Long categoryId, 
                                      @Param("brandId") Long brandId, 
                                      @Param("minPrice") BigDecimal minPrice, 
                                      @Param("maxPrice") BigDecimal maxPrice);

    /**
     * Tìm kiếm và lọc sản phẩm public (published=true và status=SELLING).
     */
    @Query("SELECT p FROM Product p WHERE p.published = true AND p.status = :status " +
           "AND (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
           "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
           "AND (:brandId IS NULL OR p.brand.brandId = :brandId) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice)")
    List<Product> searchAndFilterPublicSelling(@Param("name") String name, 
                                             @Param("categoryId") Long categoryId, 
                                             @Param("brandId") Long brandId, 
                                             @Param("minPrice") BigDecimal minPrice, 
                                             @Param("maxPrice") BigDecimal maxPrice,
                                             @Param("status") ProductStatus status);
}