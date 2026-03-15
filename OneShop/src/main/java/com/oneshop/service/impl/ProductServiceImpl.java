package com.oneshop.service.impl;

import com.oneshop.dto.ProductDto;
import com.oneshop.dto.VariantDto;
import com.oneshop.entity.*;
import com.oneshop.enums.ProductStatus;
import com.oneshop.repository.*;
import com.oneshop.service.BrandService;
import com.oneshop.service.CategoryService;
import com.oneshop.service.FileStorageService;
import com.oneshop.service.ProductService;
import com.oneshop.specification.ProductSpecification;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import org.hibernate.Hibernate; // Import Hibernate
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

// import java.io.IOException; // Không cần nữa nếu Exception được bọc
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);

    @Autowired private ProductRepository productRepository;
    @Autowired private ShopRepository shopRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductImageRepository productImageRepository;
    // Sử dụng @Lazy để tránh lỗi vòng lặp phụ thuộc (circular dependency)
    // vì ProductReviewRepository có thể cũng cần ProductService
    @Autowired @Lazy private ProductReviewRepository reviewRepository;
    @Autowired private FileStorageService fileStorageService;
    @Autowired private BrandService brandService;
    @Autowired private ProductVariantRepository variantRepository;
    @Autowired private CategoryService categoryService;

    // --- Các phương thức cho Vendor (Thêm, Sửa, Xóa, Lấy danh sách...) ---

    @Override
    @Transactional(readOnly = true)
    public Page<Product> getProductsByShop(Long shopId, Pageable pageable) {
        logger.debug("Fetching products for shop ID: {} with pageable: {}", shopId, pageable);
        Page<Product> productPage = productRepository.findByShopId(shopId, pageable);
        // Gọi setProductDetails cho từng sản phẩm trong trang hiện tại
        productPage.getContent().forEach(this::setProductDetails);
        return productPage;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Product> getProductByIdForVendor(Long productId, Long shopId) {
        logger.debug("Fetching product ID: {} for vendor shop ID: {}", productId, shopId);
        Optional<Product> productOpt = productRepository.findById(productId)
                .filter(product -> product.getShop() != null && product.getShop().getId().equals(shopId));
        // Nếu tìm thấy sản phẩm, khởi tạo các collection và gọi setProductDetails
        productOpt.ifPresent(p -> {
             Hibernate.initialize(p.getImages());  // Load ảnh sản phẩm
             Hibernate.initialize(p.getVariants()); // Load biến thể
             setProductDetails(p); // Tính toán và gán các trường transient
        });
        return productOpt;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Product addProduct(ProductDto productDto, List<MultipartFile> images, Long shopId) {
        logger.info("Adding new product '{}' for shop ID: {}", productDto.getProductName(), shopId);
        // Tìm các entity liên quan (Shop, Category, Brand)
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new EntityNotFoundException("Shop not found with ID: " + shopId));
        Category category = categoryRepository.findById(productDto.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found with ID: " + productDto.getCategoryId()));
        Brand brand = resolveBrand(productDto.getBrandId(), productDto.getNewBrandName()); // Xử lý Brand (chọn hoặc tạo mới)

        // Kiểm tra tính hợp lệ của các biến thể
        validateVariants(productDto.getVariants());

        List<String> savedGeneralImageFilenames = new ArrayList<>();
        List<String> savedVariantImageFilenames = new ArrayList<>();
        Product product = new Product();

        try {
            // Lưu ảnh chung (nếu có)
            savedGeneralImageFilenames.addAll(saveImages(images));
            // Gán ảnh chung đã lưu vào đối tượng Product
            setProductImages(product, savedGeneralImageFilenames);

            // Map thông tin cơ bản từ DTO sang Entity Product
            mapDtoToEntity(productDto, product, category, shop, brand);
            product.setPublished(true); // Mặc định publish khi thêm mới (Admin sẽ duyệt sau)
            product.setStatus(ProductStatus.PENDING); // Trạng thái chờ duyệt

            // Map thông tin biến thể và lưu ảnh biến thể (nếu có)
            mapAndSaveVariants(productDto.getVariants(), product, savedVariantImageFilenames);
            // Cập nhật giá và tồn kho tổng của Product dựa trên các biến thể
            updateProductPriceAndStockFromVariants(product);

            // Lưu Product (bao gồm cả Variants và Images do cascade)
            Product savedProduct = productRepository.save(product);
            logger.info("Successfully added product ID: {}", savedProduct.getProductId());
            return savedProduct;

        } catch (Exception e) {
            // Xử lý lỗi: Xóa các file ảnh đã lưu nếu quá trình lưu thất bại
            logger.error("Error during addProduct process for '{}': {}", productDto.getProductName(), e.getMessage(), e);
            deleteImageFiles(savedGeneralImageFilenames);
            deleteImageFiles(savedVariantImageFilenames);

            // Ném lại các loại Exception khác nhau để Controller xử lý phù hợp
            if (e instanceof DataIntegrityViolationException) {
                 throw new RuntimeException("Lỗi dữ liệu khi lưu sản phẩm (có thể tên sản phẩm hoặc SKU biến thể bị trùng?).", e);
            } else if (e instanceof IllegalArgumentException || e instanceof EntityNotFoundException) {
                throw e; // Ném lại lỗi validation hoặc không tìm thấy entity
            }
            // Các lỗi khác (bao gồm lỗi lưu ảnh đã được bọc trong RuntimeException)
            throw new RuntimeException("Lỗi không xác định khi thêm sản phẩm: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Product updateProduct(Long productId, ProductDto productDto, List<MultipartFile> newImages, Long shopId) {
        logger.info("Updating product ID: {} for shop ID: {}", productId, shopId);
        // Tìm sản phẩm hiện có, đảm bảo thuộc đúng shop
        Product existingProduct = productRepository.findById(productId)
                .filter(p -> p.getShop() != null && p.getShop().getId().equals(shopId))
                .orElseThrow(() -> new EntityNotFoundException("Product not found or permission denied."));
        // Load các collection trước khi cập nhật
        Hibernate.initialize(existingProduct.getVariants());
        Hibernate.initialize(existingProduct.getImages());

        // Tìm các entity liên quan
        Category category = categoryRepository.findById(productDto.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found with ID: " + productDto.getCategoryId()));
        Brand brand = resolveBrand(productDto.getBrandId(), productDto.getNewBrandName());

        // Kiểm tra biến thể
        validateVariants(productDto.getVariants());

        List<String> savedNewGeneralImageFilenames = new ArrayList<>();
        List<String> savedNewVariantImageFilenames = new ArrayList<>();
        List<String> oldVariantImagesToDelete = new ArrayList<>(); // Danh sách ảnh cũ cần xóa

        try {
            // Lưu ảnh chung mới (nếu có)
            savedNewGeneralImageFilenames.addAll(saveImages(newImages));
            // Map thông tin cơ bản
            mapDtoToEntity(productDto, existingProduct, category, existingProduct.getShop(), brand);
            // Khi cập nhật, luôn đặt lại trạng thái chờ duyệt
            existingProduct.setStatus(ProductStatus.PENDING);

            // Cập nhật/Thêm/Xóa biến thể và ảnh biến thể
            updateAndSaveVariants(productDto.getVariants(), existingProduct, savedNewVariantImageFilenames, oldVariantImagesToDelete);
            // Cập nhật lại giá/tồn kho tổng
            updateProductPriceAndStockFromVariants(existingProduct);

            // Gán ảnh chung mới vào sản phẩm (nếu có ảnh mới)
            if (!savedNewGeneralImageFilenames.isEmpty()) {
                // Có thể cần logic xóa ảnh chung cũ ở đây nếu muốn thay thế hoàn toàn
                // Ví dụ: deleteImageFiles(existingProduct.getImages().stream().map(ProductImage::getImageUrl).collect(Collectors.toList()));
                // existingProduct.getImages().clear(); // Xóa liên kết ảnh cũ
                setProductImages(existingProduct, savedNewGeneralImageFilenames);
            }

            // Lưu sản phẩm đã cập nhật
            Product updatedProduct = productRepository.save(existingProduct);
            // Xóa các file ảnh biến thể cũ không còn dùng nữa
            deleteImageFiles(oldVariantImagesToDelete);

            logger.info("Successfully updated product ID: {}", updatedProduct.getProductId());
            return updatedProduct;

        } catch (Exception e) {
            // Xử lý lỗi: Xóa ảnh mới đã lưu nếu có lỗi
            logger.error("Error during updateProduct process for ID {}: {}", productId, e.getMessage(), e);
            deleteImageFiles(savedNewGeneralImageFilenames);
            deleteImageFiles(savedNewVariantImageFilenames);

            // Ném lại Exception
             if (e instanceof DataIntegrityViolationException) {
                 throw new RuntimeException("Lỗi dữ liệu khi cập nhật sản phẩm (có thể trùng SKU?).", e);
            } else if (e instanceof IllegalArgumentException || e instanceof EntityNotFoundException) {
                throw e;
            }
            throw new RuntimeException("Lỗi không xác định khi cập nhật sản phẩm: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void deleteProduct(Long productId, Long shopId) {
        logger.warn("Attempting to delete product ID: {} for shop ID: {}", productId, shopId);
        // Tìm sản phẩm và kiểm tra quyền sở hữu
        Product product = productRepository.findById(productId)
                .filter(p -> p.getShop() != null && p.getShop().getId().equals(shopId))
                .orElseThrow(() -> new EntityNotFoundException("Product not found or permission denied."));

        // Load các collection để lấy danh sách file cần xóa
        Hibernate.initialize(product.getImages());
        Hibernate.initialize(product.getVariants());

        List<String> imageFilenamesToDelete = new ArrayList<>();
        // Lấy URL ảnh chung
        if (product.getImages() != null) {
            product.getImages().stream()
                   .map(ProductImage::getImageUrl)
                   .filter(StringUtils::hasText) // Chỉ lấy URL có giá trị
                   .forEach(imageFilenamesToDelete::add);
        }
        // Lấy URL ảnh biến thể
        if (product.getVariants() != null) {
            product.getVariants().stream()
                   .map(ProductVariant::getImageUrl)
                   .filter(StringUtils::hasText) // Chỉ lấy URL có giá trị
                   .forEach(imageFilenamesToDelete::add);
        }

        try {
            // Xóa entity Product (cascade sẽ xóa Images và Variants)
            productRepository.delete(product);
            // Đảm bảo lệnh xóa được gửi đi (tùy chọn, có thể không cần)
            productRepository.flush();
            logger.info("Successfully initiated deletion for product entity ID: {} from database.", productId);

            // Sau khi xóa entity thành công, tiến hành xóa file vật lý
            deleteImageFiles(imageFilenamesToDelete);
            logger.info("Attempted deletion of associated image files from storage for product {}.", productId);

        } catch (DataIntegrityViolationException e) {
             // Lỗi nếu sản phẩm đã có trong đơn hàng hoặc liên kết khác
             logger.error("ConstraintViolationException deleting product ID {}: {}", productId, e.getMessage());
             throw new RuntimeException("Không thể xóa sản phẩm này vì nó đã tồn tại trong đơn hàng hoặc có liên kết dữ liệu khác.", e);
        } catch (Exception e) {
             // Các lỗi khác
             logger.error("Unexpected error deleting product ID {}: {}", productId, e.getMessage(), e);
             throw new RuntimeException("Lỗi không xác định khi xóa sản phẩm: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long countProductsByShop(Long shopId) {
        logger.debug("Counting products for shop ID: {}", shopId);
        return productRepository.countByShopId(shopId);
    }

    // --- Các phương thức Public (cho User xem) ---

    @Override
    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        logger.debug("Fetching all categories sorted by name");
        return categoryRepository.findAll(Sort.by("name"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Brand> getAllBrands() {
        logger.debug("Fetching all brands via BrandService sorted by name");
        return brandService.findAll(); // Sử dụng BrandService đã được inject
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> findBestSellingProducts(int limit) {
        logger.debug("Fetching {} best selling PUBLISHED and SELLING products", limit);
        // Chỉ lấy sản phẩm đã published VÀ có trạng thái SELLING
        Specification<Product> spec = Specification.where(ProductSpecification.isPublished())
                                                  .and(ProductSpecification.isSelling());
        // Sắp xếp theo salesCount giảm dần
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "salesCount"));
        Page<Product> productPage = productRepository.findAll(spec, pageable);
        List<Product> products = productPage.getContent();
        // Tính toán và gán các trường transient cho từng sản phẩm
        products.forEach(this::setProductDetails);
        logger.info("findBestSellingProducts - Found {} products.", products.size());
        return products;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> findNewestProducts(Pageable pageable) {
        logger.debug("Fetching newest PUBLISHED and SELLING products with pageable: {}", pageable);
        // Chỉ lấy sản phẩm đã published VÀ có trạng thái SELLING
        Specification<Product> spec = Specification.where(ProductSpecification.isPublished())
                                                  .and(ProductSpecification.isSelling());
        // Đảm bảo sắp xếp theo productId giảm dần nếu không có sắp xếp nào khác được yêu cầu
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by(Sort.Direction.DESC, "productId");
        Pageable effectivePageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        Page<Product> productPage = productRepository.findAll(spec, effectivePageable);
        // Tính toán và gán các trường transient
        productPage.getContent().forEach(this::setProductDetails);
        logger.info("findNewestProducts - Found {} products on page {}.", productPage.getNumberOfElements(), effectivePageable.getPageNumber());
        return productPage;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> findBestPriceProducts(int limit) {
        logger.debug("Fetching {} best discount PUBLISHED and SELLING products", limit);
        // Lấy sản phẩm published, selling, có giá gốc và giá gốc > giá bán
        Specification<Product> spec = Specification.where(ProductSpecification.isPublished())
                                                  .and(ProductSpecification.isSelling())
                                                  .and((root, query, cb) -> cb.isNotNull(root.get("originalPrice"))) // Phải có giá gốc
                                                  .and((root, query, cb) -> cb.greaterThan(root.get("originalPrice"), root.get("price"))); // Giá gốc > giá bán

        // Sắp xếp theo giá bán tăng dần
        Sort sort = Sort.by(Sort.Direction.ASC, "price");
        Pageable pageable = PageRequest.of(0, limit, sort);
        Page<Product> productPage = productRepository.findAll(spec, pageable);

        List<Product> products = productPage.getContent();
        // Tính toán và gán các trường transient
        products.forEach(this::setProductDetails);
        logger.info("findBestPriceProducts - Found {} products.", products.size());
        return products;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Product> findProductById(Long productId) {
        logger.debug("Fetching PUBLISHED and SELLING product by ID: {}", productId);
        // Tìm sản phẩm theo ID, phải published và selling
        Specification<Product> spec = Specification.where(ProductSpecification.isPublished())
                                                  .and(ProductSpecification.isSelling())
                                                  .and((root, query, cb) -> cb.equal(root.get("productId"), productId));
        Optional<Product> productOpt = productRepository.findOne(spec);
        // Load collection và tính toán trường transient nếu tìm thấy
        productOpt.ifPresent(p -> {
            Hibernate.initialize(p.getImages());
            Hibernate.initialize(p.getVariants());
            setProductDetails(p);
        });
        return productOpt;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> findRelatedProducts(Product product, int limit) {
        // Kiểm tra đầu vào
        if (product == null || product.getCategory() == null || product.getProductId() == null) {
            logger.warn("Cannot find related products for null product, category, or productId.");
            return Collections.emptyList();
        }
        Long categoryId = product.getCategory().getId();
        Long currentProductId = product.getProductId();
        logger.debug("Fetching {} related PUBLISHED and SELLING products for product ID: {} in category ID: {}", limit, currentProductId, categoryId);

        // Điều kiện: published, selling, cùng category, khác ID hiện tại
        Specification<Product> spec = Specification.where(ProductSpecification.isPublished())
                                                  .and(ProductSpecification.isSelling())
                                                  .and(ProductSpecification.hasCategoryId(categoryId))
                                                  .and((root, query, cb) -> cb.notEqual(root.get("productId"), currentProductId));

        // Sắp xếp theo số lượng bán giảm dần (ưu tiên sản phẩm bán chạy)
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "salesCount"));
        Page<Product> productPage = productRepository.findAll(spec, pageable);

        List<Product> relatedProducts = productPage.getContent();
        // Tính toán trường transient
        relatedProducts.forEach(this::setProductDetails);
        return relatedProducts;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> findAllPublishedProducts(Specification<Product> spec, Pageable pageable) {
        logger.debug("Finding all PUBLISHED and SELLING products with spec and pageable: {}", pageable);
        // Kết hợp spec đầu vào với điều kiện published và selling
        Specification<Product> finalSpec = Specification.where(spec) // Spec từ controller (filter)
                                                      .and(ProductSpecification.isPublished())
                                                      .and(ProductSpecification.isSelling());
        Page<Product> productPage = productRepository.findAll(finalSpec, pageable);
        // Tính toán trường transient
        productPage.getContent().forEach(this::setProductDetails);
        return productPage;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> searchAndFilterPublic(String name, Long categoryId, Long brandId, BigDecimal minPrice, BigDecimal maxPrice) {
        logger.debug("Searching public SELLING products with name: {}, categoryId: {}, brandId: {}, minPrice: {}, maxPrice: {}", name, categoryId, brandId, minPrice, maxPrice);
        // Bắt đầu với điều kiện published và selling
        Specification<Product> spec = Specification.where(ProductSpecification.isPublished())
                                                  .and(ProductSpecification.isSelling());
        // Thêm các điều kiện lọc nếu có
        if (StringUtils.hasText(name)) {
            spec = spec.and(ProductSpecification.hasName(name));
        }
        if (categoryId != null) {
            spec = spec.and(ProductSpecification.hasCategoryId(categoryId));
        }
        if (brandId != null) {
            spec = spec.and(ProductSpecification.hasBrandId(brandId));
        }
        if (minPrice != null || maxPrice != null) {
            spec = spec.and(ProductSpecification.priceBetween(minPrice, maxPrice));
        }

        // Lấy danh sách sản phẩm, sắp xếp theo ID giảm dần
        List<Product> products = productRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "productId"));
        // Tính toán trường transient
        products.forEach(this::setProductDetails);
        return products;
    }

    // --- Các phương thức Helper (private) ---

    // Map DTO sang Entity (thông tin cơ bản)
    private void mapDtoToEntity(ProductDto dto, Product entity, Category category, Shop shop, Brand brand) {
        entity.setName(dto.getProductName());
        entity.setDescription(dto.getProductDescription());
        entity.setTags(dto.getProductTags());
        entity.setCategory(category);
        entity.setShop(shop);
        entity.setBrand(brand); // Gán Brand đã được xử lý
    }

    // Xử lý việc chọn Brand có sẵn hoặc tạo Brand mới
    private Brand resolveBrand(Long brandId, String newBrandName) {
        Brand brand = null;
        if (brandId != null) { // Ưu tiên chọn brand có sẵn
            brand = brandService.findById(brandId)
                    .orElseThrow(() -> new EntityNotFoundException("Brand not found with ID: " + brandId));
        } else if (StringUtils.hasText(newBrandName)) { // Nếu không chọn mà có nhập tên mới
            String trimmedBrandName = newBrandName.trim();
            // Kiểm tra xem tên mới đã tồn tại chưa (không phân biệt hoa thường)
            Optional<Brand> existingBrandOpt = brandService.findByNameIgnoreCase(trimmedBrandName);
            if (existingBrandOpt.isPresent()) {
                brand = existingBrandOpt.get(); // Dùng brand đã tồn tại
                logger.debug("Using existing brand found by name: {}", trimmedBrandName);
            } else {
                // Tạo brand mới
                Brand newBrand = new Brand();
                newBrand.setName(trimmedBrandName);
                try {
                    brand = brandService.saveBrand(newBrand); // Lưu brand mới
                    logger.info("Created new brand: {}", trimmedBrandName);
                } catch (Exception e) {
                    logger.error("Error creating new brand '{}': {}", trimmedBrandName, e.getMessage());
                    // Ném lỗi để dừng quá trình và rollback
                    throw new RuntimeException("Lỗi khi tạo thương hiệu mới: " + e.getMessage(), e);
                }
            }
        }
        // Nếu không chọn và không nhập mới, brand sẽ là null
        return brand;
    }

    // Kiểm tra tính hợp lệ của danh sách VariantDto
    private void validateVariants(List<VariantDto> variants) {
        if (variants == null || variants.isEmpty()) {
            throw new IllegalArgumentException("Sản phẩm phải có ít nhất một loại (biến thể).");
        }
        Set<String> variantNames = new HashSet<>();
        for (VariantDto dto : variants) {
            if (!StringUtils.hasText(dto.getName())) {
                 throw new IllegalArgumentException("Tên loại sản phẩm (biến thể) không được để trống.");
            }
            // Kiểm tra tên biến thể trùng lặp (không phân biệt hoa thường)
            if (!variantNames.add(dto.getName().trim().toLowerCase())) {
                 throw new IllegalArgumentException("Tên các loại sản phẩm (biến thể) không được trùng nhau: '" + dto.getName() + "'.");
            }
            if (dto.getPrice() == null || dto.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                 throw new IllegalArgumentException("Giá bán cho loại '" + dto.getName() + "' phải lớn hơn 0.");
            }
            if (dto.getOriginalPrice() != null && dto.getOriginalPrice().compareTo(BigDecimal.ZERO) < 0) {
                 throw new IllegalArgumentException("Giá gốc cho loại '" + dto.getName() + "' không được âm.");
            }
            // Bỏ kiểm tra giá gốc >= giá bán
            if (dto.getStock() == null || dto.getStock() < 0) {
                throw new IllegalArgumentException("Tồn kho cho loại '" + dto.getName() + "' không được âm.");
            }
        }
    }

    // Tạo và lưu các biến thể mới cho sản phẩm mới
    private void mapAndSaveVariants(List<VariantDto> variantDtos, Product product, List<String> savedVariantImageFilenames) {
         if (product.getVariants() == null) {
            product.setVariants(new HashSet<>());
         }
         product.getVariants().clear(); // Đảm bảo collection rỗng trước khi thêm

        for (VariantDto dto : variantDtos) {
            ProductVariant variant = new ProductVariant();
            mapVariantDtoToEntity(dto, variant); // Map thông tin cơ bản

            MultipartFile imageFile = dto.getVariantImageFile();
            if (imageFile != null && !imageFile.isEmpty()) {
                try {
                    String filename = fileStorageService.save(imageFile); // Lưu ảnh biến thể
                    variant.setImageUrl(filename); // Gán tên file đã lưu
                    savedVariantImageFilenames.add(filename); // Thêm vào danh sách để xử lý nếu có lỗi
                    logger.debug("Saved image '{}' for new variant '{}'", filename, dto.getName());
                } catch (Exception e) { // Bắt Exception chung
                    logger.error("Error saving image for new variant '{}': {}", dto.getName(), e.getMessage());
                    // Ném RuntimeException để đảm bảo rollback transaction
                    throw new RuntimeException("Lỗi khi lưu ảnh cho loại sản phẩm '" + dto.getName() + "'", e);
                }
            } else {
                 variant.setImageUrl(null); // Không có ảnh
            }
            variant.setProduct(product); // Liên kết biến thể với sản phẩm cha
            product.getVariants().add(variant); // Thêm biến thể vào collection của sản phẩm
        }
         logger.info("Mapped {} variants for the new product.", product.getVariants().size());
    }

    // Cập nhật, thêm, xóa biến thể cho sản phẩm đang chỉnh sửa
    private void updateAndSaveVariants(List<VariantDto> variantDtos, Product existingProduct, List<String> savedNewVariantImageFilenames, List<String> oldVariantImagesToDelete) {
        // Tạo Map các biến thể hiện có theo ID để dễ truy cập
        Map<Long, ProductVariant> existingVariantsMap = existingProduct.getVariants().stream()
                .collect(Collectors.toMap(ProductVariant::getVariantId, v -> v));

        Set<ProductVariant> finalVariants = new HashSet<>(); // Collection chứa các biến thể cuối cùng
        List<Long> dtoVariantIds = new ArrayList<>(); // Lưu ID các biến thể từ DTO để xác định cái nào bị xóa

        for (VariantDto dto : variantDtos) {
            ProductVariant variant;
            String oldImageUrl = null; // Lưu URL ảnh cũ của biến thể (nếu có)

            // Kiểm tra xem DTO có ID không và ID đó có trong map biến thể cũ không
            if (dto.getVariantId() != null && existingVariantsMap.containsKey(dto.getVariantId())) {
                // Đây là biến thể cũ cần cập nhật
                variant = existingVariantsMap.get(dto.getVariantId());
                oldImageUrl = variant.getImageUrl(); // Lấy URL ảnh cũ
                mapVariantDtoToEntity(dto, variant); // Cập nhật thông tin từ DTO
                dtoVariantIds.add(dto.getVariantId()); // Đánh dấu ID này có trong DTO
                logger.trace("Updating existing variant ID {}", dto.getVariantId());
            } else {
                // Đây là biến thể mới cần thêm
                variant = new ProductVariant();
                mapVariantDtoToEntity(dto, variant); // Map thông tin
                variant.setProduct(existingProduct); // Liên kết với sản phẩm cha
                logger.trace("Adding new variant '{}'", dto.getName());
            }

            // Xử lý ảnh biến thể
            MultipartFile imageFile = dto.getVariantImageFile();
            if (imageFile != null && !imageFile.isEmpty()) { // Nếu có upload file ảnh mới
                 try {
                    String filename = fileStorageService.save(imageFile); // Lưu file mới
                    variant.setImageUrl(filename); // Cập nhật URL ảnh mới cho biến thể
                    savedNewVariantImageFilenames.add(filename); // Thêm vào danh sách để xử lý nếu lỗi
                    logger.debug("Saved new image '{}' for variant '{}'", filename, dto.getName());
                    // Nếu biến thể này trước đó có ảnh, đánh dấu ảnh cũ cần xóa
                    if (StringUtils.hasText(oldImageUrl)) {
                        oldVariantImagesToDelete.add(oldImageUrl);
                        logger.trace("Marked old image '{}' for deletion.", oldImageUrl);
                    }
                } catch (Exception e) { // Bắt Exception chung
                    logger.error("Error saving NEW image for variant '{}': {}", dto.getName(), e.getMessage());
                    // Ném RuntimeException để rollback
                    throw new RuntimeException("Lỗi khi lưu ảnh mới cho loại sản phẩm '" + dto.getName() + "'", e);
                }
            } else { // Không có upload file ảnh mới
                // Kiểm tra xem DTO có giữ lại URL ảnh cũ không (existingImageUrl)
                if (StringUtils.hasText(dto.getExistingImageUrl())) {
                    // Nếu URL trong DTO khác URL cũ trong DB (trường hợp hiếm), cập nhật và xóa ảnh cũ
                    if (!dto.getExistingImageUrl().equals(oldImageUrl)) {
                         logger.warn("Existing image URL mismatch for variant {}. DTO: {}, DB: {}. Keeping DTO value.", dto.getVariantId(), dto.getExistingImageUrl(), oldImageUrl);
                         variant.setImageUrl(dto.getExistingImageUrl());
                         if (StringUtils.hasText(oldImageUrl)) { // Xóa ảnh cũ nếu có
                              oldVariantImagesToDelete.add(oldImageUrl);
                         }
                    } else {
                         // Giữ nguyên ảnh cũ nếu URL trong DTO giống trong DB
                         variant.setImageUrl(oldImageUrl);
                    }
                } else { // DTO không có existingImageUrl (nghĩa là người dùng muốn xóa ảnh)
                    variant.setImageUrl(null); // Đặt URL ảnh là null
                    // Nếu biến thể này trước đó có ảnh, đánh dấu ảnh cũ cần xóa
                    if (StringUtils.hasText(oldImageUrl)) {
                        oldVariantImagesToDelete.add(oldImageUrl);
                        logger.trace("Marked old image '{}' for deletion (user removed).", oldImageUrl);
                    }
                }
            }
            finalVariants.add(variant); // Thêm biến thể đã xử lý vào collection cuối cùng
        }

        // Xác định các biến thể cũ bị xóa (có trong existingVariantsMap nhưng không có trong dtoVariantIds)
        existingProduct.getVariants().stream()
            .filter(v -> !dtoVariantIds.contains(v.getVariantId())) // Lọc ra những cái bị xóa
            .forEach(v -> {
                logger.warn("Variant ID {} ('{}') is marked for deletion.", v.getVariantId(), v.getName());
                // Nếu biến thể bị xóa có ảnh, đánh dấu ảnh đó cần xóa file
                if (StringUtils.hasText(v.getImageUrl())) {
                    oldVariantImagesToDelete.add(v.getImageUrl());
                    logger.trace("Marked image '{}' of deleted variant for file deletion.", v.getImageUrl());
                }
            });

        // Cập nhật collection variants của sản phẩm
        existingProduct.getVariants().clear(); // Xóa hết liên kết cũ
        existingProduct.getVariants().addAll(finalVariants); // Thêm lại các biến thể cuối cùng
        logger.info("Updated variants collection for product. Final count: {}", existingProduct.getVariants().size());
    }

    // Map thông tin từ VariantDto sang ProductVariant entity
    private void mapVariantDtoToEntity(VariantDto dto, ProductVariant entity) {
        entity.setName(dto.getName());
        entity.setSku(dto.getSku());
        entity.setPrice(dto.getPrice());
        entity.setOriginalPrice(dto.getOriginalPrice());
        entity.setStock(dto.getStock());
        entity.setActive(true); // Mặc định là active
    }

    // Tính toán và cập nhật giá/tồn kho tổng của Product (KHÔNG lưu vào DB)
    private void updateProductPriceAndStockFromVariants(Product product) {
        if (product == null) return;
        Set<ProductVariant> variants = product.getVariants();
        // Nếu không có biến thể, đặt giá/kho về 0
        if (variants == null || variants.isEmpty()) {
            product.setPrice(BigDecimal.ZERO);
            product.setOriginalPrice(null);
            product.setStock(0);
            return;
        }

        // Tìm giá bán thấp nhất trong các biến thể
        BigDecimal minPrice = variants.stream()
               .map(ProductVariant::getPrice)
               .filter(Objects::nonNull) // Bỏ qua giá null
               .min(BigDecimal::compareTo) // Tìm giá nhỏ nhất
               .orElse(BigDecimal.ZERO); // Nếu không có giá nào thì là 0

        // Tìm giá gốc tương ứng với biến thể có giá bán thấp nhất (lấy giá gốc lớn nhất nếu có nhiều biến thể cùng giá min)
        BigDecimal correspondingOriginalPrice = variants.stream()
               .filter(v -> v.getPrice() != null && v.getPrice().compareTo(minPrice) == 0 && v.getOriginalPrice() != null) // Lọc biến thể có giá min và có giá gốc
               .map(ProductVariant::getOriginalPrice)
               .max(BigDecimal::compareTo) // Lấy giá gốc lớn nhất
               .orElse(null); // Nếu không có giá gốc nào thì là null

        // Tính tổng tồn kho của tất cả biến thể
        int totalStock = variants.stream()
                           .mapToInt(ProductVariant::getStock)
                           .sum();

        // Gán giá trị đã tính vào Product
        product.setPrice(minPrice);
        product.setOriginalPrice(correspondingOriginalPrice);
        product.setStock(totalStock);
    }

    // Lưu danh sách file ảnh (MultipartFile) và trả về danh sách tên file đã lưu
    private List<String> saveImages(List<MultipartFile> images) {
         if (images == null || images.isEmpty()) {
            return Collections.emptyList(); // Trả về list rỗng nếu không có ảnh
         }
        List<String> savedFilenames = new ArrayList<>();
        for(MultipartFile file : images) {
            // Chỉ xử lý file không rỗng
            if (file != null && !file.isEmpty()) {
                try {
                    String filename = fileStorageService.save(file); // Gọi service lưu file
                    savedFilenames.add(filename); // Thêm tên file đã lưu vào list
                    logger.trace("Saved image file: {}", filename);
                } catch (Exception e) { // Bắt Exception chung
                    logger.error("Error saving image file '{}': {}", file.getOriginalFilename(), e.getMessage());
                    // Ném RuntimeException để dừng và rollback
                    throw new RuntimeException("Lỗi khi lưu file ảnh: " + file.getOriginalFilename(), e);
                }
            }
        }
        logger.debug("Saved {} image files.", savedFilenames.size());
        return savedFilenames;
    }

     // Xóa danh sách file ảnh vật lý khỏi hệ thống lưu trữ
     private void deleteImageFiles(List<String> filenames) {
         if (filenames != null && !filenames.isEmpty()) {
             logger.warn("Attempting to delete {} image files from storage.", filenames.size());
            filenames.forEach(filename -> {
                // Chỉ xóa nếu tên file có giá trị
                if (StringUtils.hasText(filename)) {
                    try {
                        fileStorageService.delete(filename); // Gọi service xóa file
                        logger.trace("Deleted image file: {}", filename);
                    } catch (Exception e) { // Bắt Exception chung
                        // Ghi log lỗi nhưng không dừng chương trình
                        logger.error("Error deleting image file '{}' from storage: {}", filename, e.getMessage());
                    }
                }
            });
        }
    }

    // Tạo và gán các đối tượng ProductImage vào Product từ danh sách tên file
    private void setProductImages(Product product, List<String> imageFilenames) {
        if (imageFilenames != null && !imageFilenames.isEmpty()) {
            if (product.getImages() == null) {
                product.setImages(new HashSet<>()); // Khởi tạo collection nếu null
            }
            // Kiểm tra xem sản phẩm đã có ảnh chính chưa
            boolean needsPrimary = product.getImages().stream().noneMatch(img -> Boolean.TRUE.equals(img.getIsPrimary()));

            for (int i = 0; i < imageFilenames.size(); i++) {
                ProductImage img = new ProductImage();
                img.setProduct(product); // Liên kết với sản phẩm cha
                img.setImageUrl(imageFilenames.get(i)); // Gán URL (tên file)
                // Ảnh đầu tiên trong danh sách mới sẽ là ảnh chính NẾU sản phẩm chưa có ảnh chính
                img.setIsPrimary(needsPrimary && i == 0);
                product.getImages().add(img); // Thêm vào collection của sản phẩm
            }
             logger.debug("Added {} new images to product. First image set as primary: {}", imageFilenames.size(), needsPrimary && !imageFilenames.isEmpty());
        }
    }

    // === PHƯƠNG THỨC MỚI ĐỂ TÍNH TOÁN URL ẢNH CHÍNH ===
    @Override
    public String calculatePrimaryImageUrl(Product product) {
        if (product == null) {
            return "/assets/img/product/no-image.jpg"; // Trả về ảnh mặc định nếu product null
        }
        final String defaultImageUrl = "/assets/img/product/no-image.jpg";
        String determinedPrimaryImageUrl = defaultImageUrl;

        try {
            // Đảm bảo các collection được load (quan trọng nếu chúng là LAZY)
            Hibernate.initialize(product.getImages());
            Hibernate.initialize(product.getVariants());

            // Ưu tiên ảnh từ collection Product.images
            if (product.getImages() != null && !product.getImages().isEmpty()) {
                // 1. Tìm ảnh có đánh dấu isPrimary = true
                determinedPrimaryImageUrl = product.getImages().stream()
                    .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()) && StringUtils.hasText(img.getImageUrl()))
                    .map(ProductImage::getImageUrl) // Lấy URL
                    .findFirst() // Lấy cái đầu tiên tìm thấy
                    // 2. Nếu không có ảnh isPrimary, lấy ảnh đầu tiên trong danh sách images
                    .orElseGet(() ->
                        product.getImages().stream()
                            .map(ProductImage::getImageUrl)
                            .filter(StringUtils::hasText) // Đảm bảo URL không rỗng
                            .findFirst() // Lấy cái đầu tiên
                            .orElse(defaultImageUrl) // Nếu vẫn không có thì dùng ảnh mặc định
                    );
            }
            // Nếu không có ảnh nào trong Product.images, thử lấy ảnh từ biến thể đầu tiên
            else if (product.getVariants() != null && !product.getVariants().isEmpty()) {
                determinedPrimaryImageUrl = product.getVariants().stream()
                    .map(ProductVariant::getImageUrl) // Lấy URL ảnh của biến thể
                    .filter(StringUtils::hasText) // Chỉ lấy URL có giá trị
                    .findFirst() // Lấy URL của biến thể đầu tiên có ảnh
                    .orElse(defaultImageUrl); // Nếu không có biến thể nào có ảnh, dùng ảnh mặc định
            }

            // Quan trọng: Thêm tiền tố path '/uploads/images/' nếu URL trả về chỉ là tên file
            if (!determinedPrimaryImageUrl.equals(defaultImageUrl) && !determinedPrimaryImageUrl.startsWith("/")) {
                 // Giả sử FileStorageService trả về tên file, cần thêm path
                determinedPrimaryImageUrl = "/uploads/images/" + determinedPrimaryImageUrl;
            }
        } catch (Exception e) {
            // Ghi log lỗi nếu có vấn đề khi truy cập collection (ví dụ: LazyInitializationException)
            logger.error("Error calculating primary image URL for product {}: {}",
                       product.getProductId() != null ? product.getProductId() : "new product", e.getMessage());
            determinedPrimaryImageUrl = defaultImageUrl; // Trả về ảnh mặc định khi có lỗi
        }
        return determinedPrimaryImageUrl;
    }


    // Hàm helper để gọi setProductDetails cho danh sách
    private void setProductDetails(List<Product> products) {
        if (products != null) {
            products.forEach(this::setProductDetails);
        }
    }

    // Hàm tính toán và gán giá trị cho các trường @Transient của Product
    private void setProductDetails(Product product) {
        if (product == null) {
            return;
        }

        // === SỬ DỤNG HÀM MỚI ĐỂ LẤY URL ẢNH CHÍNH ===
        product.setPrimaryImageUrl(calculatePrimaryImageUrl(product));
        logger.trace("Set primaryImageUrl for product {}: {}", product.getProductId() != null ? product.getProductId() : "new product", product.getPrimaryImageUrl());

        // --- Logic tính Rating và Review Count ---
        Double avgRating = 0.0;
        Integer reviewCount = 0;

        // Chỉ tính nếu sản phẩm đã được lưu (có ID) và reviewRepository có sẵn
        if (product.getProductId() != null && reviewRepository != null) {
            try {
                // Lấy rating trung bình từ repository (có thể trả về null)
                Double fetchedAvgRating = reviewRepository.findAverageRatingByProductId(product.getProductId());
                avgRating = (fetchedAvgRating != null) ? fetchedAvgRating : 0.0; // Gán 0.0 nếu null

                // Đếm số lượng review từ repository (có thể trả về null)
                Integer fetchedReviewCount = reviewRepository.countReviewsByProductId(product.getProductId());
                reviewCount = (fetchedReviewCount != null) ? fetchedReviewCount : 0; // Gán 0 nếu null

            } catch (Exception e) {
                // Ghi log nếu có lỗi khi truy vấn DB nhưng không dừng chương trình
                logger.warn("Could not set rating/review details for product {}: {}", product.getProductId(), e.getMessage());
                avgRating = 0.0; // Đặt giá trị mặc định khi lỗi
                reviewCount = 0;
            }
        }

        // Gán giá trị đã tính vào các trường transient
        product.setRating(avgRating);
        product.setReviewCount(reviewCount);
        // Gán soldCount từ salesCount (trường được lưu trong DB)
        product.setSoldCount(product.getSalesCount());
    }


    @Override
    @Transactional
    public void updateProductStockAndPriceFromVariants(Product product) {
        if (product == null || product.getProductId() == null) {
            logger.warn("Attempted to update stock/price for a null or unsaved product.");
            return;
        }

        // Lấy lại entity từ DB để đảm bảo đang làm việc với phiên bản mới nhất
        Product managedProduct = productRepository.findById(product.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm để cập nhật kho/giá: " + product.getProductId()));

        // Load collection variants
        Hibernate.initialize(managedProduct.getVariants());
        // Gọi hàm helper để tính toán giá trị mới (KHÔNG lưu ở đây)
        updateProductPriceAndStockFromVariants(managedProduct);
        // Lưu lại entity đã cập nhật
        productRepository.save(managedProduct);
        logger.debug("Updated aggregate stock/price for product ID: {}", managedProduct.getProductId());
    }

    // --- Các phương thức cho Admin ---

    @Override
    @Transactional
    public void deleteById(Long productId) {
        logger.warn("ADMIN action: Attempting to delete product ID: {}", productId);
        // Tìm sản phẩm
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + productId));

        // Load collection để lấy danh sách ảnh cần xóa file
        List<String> imageFilenamesToDelete = new ArrayList<>();
        Hibernate.initialize(product.getImages());
        Hibernate.initialize(product.getVariants());

        if (product.getImages() != null) {
            product.getImages().stream()
                   .map(ProductImage::getImageUrl)
                   .filter(StringUtils::hasText)
                   .forEach(imageFilenamesToDelete::add);
        }
        if (product.getVariants() != null) {
            product.getVariants().stream()
                   .map(ProductVariant::getImageUrl)
                   .filter(StringUtils::hasText)
                   .forEach(imageFilenamesToDelete::add);
        }

        try {
            // Xóa entity Product (cascade sẽ xóa Variants và Images liên quan trong DB)
            productRepository.delete(product);
            productRepository.flush(); // Đẩy lệnh xóa xuống DB (tùy chọn)
            logger.info("ADMIN action: Successfully deleted product entity ID: {}", productId);
            // Sau khi xóa entity thành công, xóa file ảnh vật lý
            deleteImageFiles(imageFilenamesToDelete);

        } catch (DataIntegrityViolationException e) {
            // Lỗi ràng buộc khóa ngoại (ví dụ: sản phẩm có trong đơn hàng)
            logger.error("ADMIN action: ConstraintViolationException deleting product ID {}: {}", productId, e.getMessage());
            throw new RuntimeException("Không thể xóa sản phẩm đã tồn tại trong đơn hàng hoặc có liên kết dữ liệu khác.", e);
        } catch (Exception e) {
            // Các lỗi khác
            logger.error("ADMIN action: Unexpected error deleting product ID {}: {}", productId, e.getMessage(), e);
            throw new RuntimeException("Lỗi không xác định khi xóa sản phẩm: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long countProductsByCategory(Long categoryId) {
        logger.debug("Counting products for category ID: {}", categoryId);
        if (categoryId == null) {
            return 0; // Trả về 0 nếu categoryId là null
        }
        return productRepository.countByCategoryId(categoryId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> findFilteredProducts(Long shopId, String productCode, ProductStatus status, Long categoryId, String brandName) {
        logger.debug("Admin filtering products for shopId: {}, code: {}, status: {}, category: {}, brand: {}",
            shopId, productCode, status, categoryId, brandName);

        // Xây dựng Specification (bộ điều kiện lọc)
        Specification<Product> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            // Luôn lọc theo shopId
            predicates.add(criteriaBuilder.equal(root.get("shop").get("id"), shopId));

            // Lọc theo Mã SP (nếu có và là số hợp lệ)
            if (StringUtils.hasText(productCode)) {
                try {
                    Long prodId = Long.parseLong(productCode.trim());
                    predicates.add(criteriaBuilder.equal(root.get("productId"), prodId));
                } catch (NumberFormatException e) {
                     // Bỏ qua nếu mã nhập vào không phải số
                     logger.warn("Invalid productCode format received: '{}'. Ignoring code filter.", productCode);
                }
            }
            // Lọc theo Trạng thái (nếu có)
            if (status != null) {
                 predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            // Lọc theo Danh mục (nếu có)
            if (categoryId != null) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("id"), categoryId));
            }
            // Lọc theo Tên thương hiệu (nếu có)
            if (StringUtils.hasText(brandName)) {
                 // So sánh không phân biệt hoa thường
                 predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("brand").get("name")), brandName.toLowerCase()));
            }

            // Kết hợp các điều kiện bằng AND
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        // Tìm tất cả sản phẩm khớp với điều kiện, sắp xếp theo ID giảm dần
        List<Product> products = productRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "productId"));
        // Tính toán các trường transient
        products.forEach(this::setProductDetails);
        return products;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> findAllUniqueBrands() {
        logger.debug("Fetching all unique brand names from BrandService");
         // Lấy tất cả Brand từ BrandService, chuyển thành Set các tên duy nhất
         return brandService.findAll().stream()
                 .map(Brand::getName) // Lấy tên
                 .filter(StringUtils::hasText) // Bỏ qua tên rỗng
                 .collect(Collectors.toCollection(LinkedHashSet::new)); // Dùng LinkedHashSet để giữ thứ tự (nếu cần)
    }

    @Override
    @Transactional
    public void updateStatus(Long productId, ProductStatus newStatus) {
        logger.info("ADMIN action: Updating status for product ID: {} to {}", productId, newStatus);
        // Tìm sản phẩm
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + productId));

        // Cập nhật trạng thái
         product.setStatus(newStatus);
         // Cập nhật trạng thái published dựa trên trạng thái mới
         product.setPublished(newStatus == ProductStatus.SELLING);

        // Lưu lại
         productRepository.save(product);
        logger.info("ADMIN action: Product {} status updated to {}. Published state: {}", productId, newStatus, product.isPublished());
    }

    @Override
    @Transactional
    public void updateAdminFields(Product productData) {
        // Kiểm tra đầu vào
        if (productData == null || productData.getProductId() == null) {
            throw new IllegalArgumentException("Product data or Product ID cannot be null for admin update.");
        }
        Long productId = productData.getProductId();
        logger.info("ADMIN action: Updating admin-editable fields for product ID: {}", productId);

        // Tìm sản phẩm hiện có
        Product existingProduct = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + productId));

        // Cập nhật các trường được phép
        existingProduct.setName(productData.getName());
        existingProduct.setDescription(productData.getDescription());

        // Cập nhật category (nếu có và hợp lệ)
        if (productData.getCategory() != null && productData.getCategory().getId() != null) {
            // Tìm category mới từ DB
            Category category = categoryService.findById(productData.getCategory().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Category not found with ID: " + productData.getCategory().getId()));
            existingProduct.setCategory(category); // Gán category mới
        } else {
             existingProduct.setCategory(null); // Hoặc gán category mặc định nếu cần
        }

        // Lưu lại sản phẩm đã cập nhật
        productRepository.save(existingProduct);
        logger.info("ADMIN action: Product {} admin fields updated successfully.", productId);
    }

    @Override
    @Transactional
    public int updateCategoryForProducts(Long oldCategoryId, Long newCategoryId) {
        logger.warn("ADMIN action: Moving products from category {} to category {}", oldCategoryId, newCategoryId);

        Category newCategory = null; // Category mới để gán

        // Kiểm tra xem có phải chuyển về "Chưa phân loại" không
        // Sử dụng hằng số UNCATEGORIZED_CATEGORY_ID từ CategoryService
        boolean setToUncategorized = newCategoryId != null && newCategoryId.equals(CategoryService.UNCATEGORIZED_CATEGORY_ID);

        if (!setToUncategorized) {
            // Nếu không phải "Chưa phân loại", tìm Category mới trong DB
            newCategory = categoryRepository.findById(newCategoryId)
                    .orElseThrow(() -> new EntityNotFoundException("Replacement category not found: " + newCategoryId));
        } else {
             // Nếu là "Chưa phân loại", newCategory sẽ là null
             logger.info("ADMIN action: Products will be moved to 'Uncategorized' (category set to null).");
        }

        // Tìm tất cả sản phẩm thuộc category cũ
        List<Product> productsToUpdate = productRepository.findByCategory_Id(oldCategoryId);

        if (!productsToUpdate.isEmpty()) {
            logger.info("Found {} products in category {} to move.", productsToUpdate.size(), oldCategoryId);
            // Cập nhật category cho từng sản phẩm
            for (Product product : productsToUpdate) {
                product.setCategory(newCategory); // Gán category mới (hoặc null)
            }
            // Lưu lại tất cả sản phẩm đã cập nhật
            productRepository.saveAll(productsToUpdate);
            logger.info("ADMIN action: Moved {} products successfully to category ID {}", productsToUpdate.size(), newCategoryId);
            return productsToUpdate.size(); // Trả về số lượng sản phẩm đã di chuyển
        } else {
            // Không tìm thấy sản phẩm nào
            logger.info("ADMIN action: No products found in category {} to move.", oldCategoryId);
            return 0; // Trả về 0
        }
    }

} // Kết thúc class ProductServiceImpl