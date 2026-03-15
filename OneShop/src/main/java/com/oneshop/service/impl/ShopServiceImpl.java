package com.oneshop.service.impl;

import com.oneshop.dto.ShopDto;
import com.oneshop.entity.Role;
import com.oneshop.entity.Shop;
import com.oneshop.entity.User;
import com.oneshop.enums.ShopStatus;
import com.oneshop.repository.RoleRepository;
import com.oneshop.repository.ShopRepository;
import com.oneshop.repository.UserRepository;
import com.oneshop.service.FileStorageService;
import com.oneshop.service.ShopService;

import jakarta.persistence.EntityNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShopServiceImpl implements ShopService {

    private static final Logger logger = LoggerFactory.getLogger(ShopServiceImpl.class);

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private RoleRepository roleRepository;

    @Override
    @Transactional(readOnly = true)
    public Shop getShopByUserId(Long userId) {
        logger.debug("Fetching shop by userId: {}", userId);
        return shopRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng này chưa có shop"));
    }

    @Override
    @Transactional
    public Shop updateShop(Long shopId, ShopDto shopDto, MultipartFile logoFile, MultipartFile bannerFile) {
        logger.info("Updating shop with ID: {}", shopId);
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy shop"));

        shop.setName(shopDto.getShopName());
        shop.setDescription(shopDto.getShopDescription());
        shop.setContactEmail(shopDto.getContactEmail());
        shop.setContactPhone(shopDto.getContactPhone());

        if (logoFile != null && !logoFile.isEmpty()) {
            logger.debug("Processing new logo file for shop {}", shopId);
            if (StringUtils.hasText(shop.getLogo())) {
                 try {
                    fileStorageService.delete(shop.getLogo());
                    logger.debug("Deleted old logo: {}", shop.getLogo());
                 } catch (Exception e) {
                     logger.error("Error deleting old logo file {}: {}", shop.getLogo(), e.getMessage());
                 }
            }
            String logoFilename = fileStorageService.save(logoFile);
            shop.setLogo(logoFilename);
            logger.debug("Saved new logo as: {}", logoFilename);
        }

        if (bannerFile != null && !bannerFile.isEmpty()) {
            logger.debug("Processing new banner file for shop {}", shopId);
            if (StringUtils.hasText(shop.getBanner())) {
                 try {
                    fileStorageService.delete(shop.getBanner());
                    logger.debug("Deleted old banner: {}", shop.getBanner());
                 } catch (Exception e) {
                      logger.error("Error deleting old banner file {}: {}", shop.getBanner(), e.getMessage());
                 }
            }
            String bannerFilename = fileStorageService.save(bannerFile);
            shop.setBanner(bannerFilename);
             logger.debug("Saved new banner as: {}", bannerFilename);
        }

        return shopRepository.save(shop);
    }

    @Override
    @Transactional
    public Shop registerShop(ShopDto shopDto, Long userId) {
        logger.info("Registering new shop for userId: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + userId));

        if(shopRepository.findByUserId(userId).isPresent()) {
            logger.warn("User {} already has a shop. Registration aborted.", userId);
            throw new RuntimeException("Người dùng này đã đăng ký shop rồi");
        }

        Shop shop = new Shop();
        shop.setName(shopDto.getShopName());
        shop.setDescription(shopDto.getShopDescription());
        shop.setContactEmail(shopDto.getContactEmail());
        shop.setContactPhone(shopDto.getContactPhone());
        shop.setUser(user);
        shop.setStatus(ShopStatus.PENDING);

        Shop savedShop = shopRepository.save(shop);
        logger.info("Shop {} registered successfully for user {} with status PENDING", savedShop.getId(), userId);

        try {
            Role vendorRole = roleRepository.findByName(Role.RoleName.VENDOR)
                    .orElseThrow(() -> {
                        logger.error("FATAL: VENDOR role not found in database!");
                        return new RuntimeException("Lỗi hệ thống: Không tìm thấy vai trò VENDOR.");
                    });

            user.setRole(vendorRole);
            user.setShop(savedShop);

            userRepository.save(user);
            logger.info("Updated user {} role to VENDOR successfully.", userId);

        } catch (Exception e) {
            logger.error("Error updating user role to VENDOR for userId {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Không thể cập nhật vai trò người dùng sau khi đăng ký shop.", e);
        }

        return savedShop;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Shop> findAllActiveShops() {
        logger.debug("Finding all active (APPROVED) shops.");
        return shopRepository.findByStatus(ShopStatus.APPROVED, Sort.by("name"));
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Shop> findAll() {
        logger.debug("Finding all shops.");
        return shopRepository.findAll(Sort.by("name"));
    }

    @Override
    @Transactional(readOnly = true)
    public Shop findById(Long shopId) {
        logger.debug("Finding shop by ID: {}", shopId);
        return shopRepository.findById(shopId).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Shop> getApprovedShops() {
        logger.debug("Finding all approved shops.");
        return shopRepository.findByStatus(ShopStatus.APPROVED, Sort.by("name"));
    }

    @Override
    @Transactional
    public Shop updateShopCommissionRate(Long shopId, BigDecimal newRate) {
        logger.info("Updating commission rate for shop ID: {} to {}", shopId, newRate);
        Shop shop = findById(shopId);
        if (shop == null) {
            throw new EntityNotFoundException("Không tìm thấy Shop với ID: " + shopId);
        }
        shop.setCommissionRate(newRate);
        shop.setCommissionUpdatedAt(LocalDateTime.now());
        return shopRepository.save(shop);
    }

    @Override
    @Transactional
    public Shop updateShopStatus(Long shopId, ShopStatus newStatus) {
        logger.info("Updating status for shop ID: {} to {}", shopId, newStatus);
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy Shop với ID: " + shopId));
        shop.setStatus(newStatus);
        return shopRepository.save(shop);
    }

}