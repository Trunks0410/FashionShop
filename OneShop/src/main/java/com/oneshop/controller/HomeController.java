// src/main/java/com/oneshop/controller/HomeController.java
package com.oneshop.controller;

import com.oneshop.entity.Product;
import com.oneshop.entity.Promotion;
import com.oneshop.entity.Shop;
import com.oneshop.service.ProductService;
import com.oneshop.service.PromotionService;
import com.oneshop.service.ShopService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;

@Controller
public class HomeController {

    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    @Autowired
    private ProductService productService;

    @Autowired
    private ShopService shopService;
    
    @Autowired
    private PromotionService promotionService;

    @GetMapping("/")
    public String index(Model model) {
        logger.info("Accessing homepage");
        int limit = 12;
        int otherLimit = 10;

        try {
            List<Product> bestSellers = productService.findBestSellingProducts(limit);
            
            // Sửa lỗi logic: Gọi đúng hàm findNewestProducts
            Pageable newestPageable = PageRequest.of(0, otherLimit, Sort.by("productId").descending());
            List<Product> newest = productService.findNewestProducts(newestPageable).getContent();
            
            List<Product> bestPrice = productService.findBestPriceProducts(otherLimit);

            model.addAttribute("bestSellingProducts", bestSellers);
            model.addAttribute("newestProducts", newest);
            model.addAttribute("bestPriceProducts", bestPrice);

            logger.debug("Loaded {} best sellers, {} newest, {} best price products for homepage.",
                         bestSellers.size(), newest.size(), bestPrice.size());

        } catch (Exception e) {
             logger.error("Error fetching products for homepage: {}", e.getMessage(), e);
             model.addAttribute("bestSellingProducts", Collections.emptyList());
             model.addAttribute("newestProducts", Collections.emptyList());
             model.addAttribute("bestPriceProducts", Collections.emptyList());
             model.addAttribute("errorMessage", "Không thể tải danh sách sản phẩm nổi bật.");
        }

        return "guest/index";
    }

    @GetMapping("/contact")
    public String contact(Model model) { 
        logger.debug("Accessing contact page (store locator)");
        try {
            List<Shop> shops = shopService.findAllActiveShops(); 
            model.addAttribute("shops", shops);
            logger.info("Loaded {} shops for contact page.", shops.size());
        } catch (Exception e) {
            logger.error("Error fetching shops for contact page: {}", e.getMessage(), e);
            model.addAttribute("shops", Collections.emptyList());
            model.addAttribute("errorMessage", "Không thể tải danh sách cửa hàng.");
        }
        return "guest/contact";
    }

    @GetMapping("/news")
    public String news(Model model,
                       @RequestParam(name = "promoPage", defaultValue = "0") int promoPage,
                       @RequestParam(name = "productPage", defaultValue = "0") int productPage) {

        logger.debug("Accessing news page - promoPage: {}, productPage: {}", promoPage, productPage);
        int pageSize = 5; 

        try {
            Pageable promoPageable = PageRequest.of(promoPage, pageSize, Sort.by("startDate").descending());
            Page<Promotion> promotionPage = promotionService.findActiveAndUpcomingPromotions(promoPageable);

            Pageable productPageable = PageRequest.of(productPage, pageSize, Sort.by("productId").descending());
            Page<Product> newestProductPage = productService.findNewestProducts(productPageable);

            model.addAttribute("promotionPage", promotionPage);
            model.addAttribute("newestProductPage", newestProductPage);
            
            model.addAttribute("currentPromoPage", promoPage);
            model.addAttribute("currentProductPage", productPage);

            logger.info("Loaded {} promotions (page {}) and {} new products (page {}) for news page.",
                    promotionPage.getNumberOfElements(), promoPage,
                    newestProductPage.getNumberOfElements(), productPage);

        } catch (Exception e) {
            logger.error("Error fetching news page content: {}", e.getMessage(), e);
            model.addAttribute("promotionPage", Page.empty()); 
            model.addAttribute("newestProductPage", Page.empty());
            model.addAttribute("errorMessage", "Không thể tải tin tức mới.");
        }

        return "guest/news";
    }

    // Phương thức mới cho chính sách vận chuyển & đổi trả
    @GetMapping("/shipping-policy")
    public String shippingPolicy(Model model) {
        logger.info("Accessing shipping and return policy page.");
        return "guest/shipping-policy";
    }
}