package com.oneshop.config; // Hoặc package phù hợp

import com.oneshop.entity.Role;
import com.oneshop.entity.Role.RoleName;
import com.oneshop.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional; // Import Transactional

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private RoleRepository roleRepository;

    @Override
    @Transactional // Quan trọng: Đảm bảo hoạt động trong một giao dịch
    public void run(String... args) throws Exception {
        logger.info("Initializing roles...");

        // Lặp qua từng RoleName trong Enum
        for (RoleName roleName : RoleName.values()) {
            // Kiểm tra xem role đã tồn tại trong CSDL chưa
            if (roleRepository.findByName(roleName).isEmpty()) {
                // Nếu chưa tồn tại, tạo mới và lưu lại
                Role newRole = new Role(roleName);
                roleRepository.save(newRole);
                logger.info("Created role: {}", roleName);
            } else {
                logger.debug("Role {} already exists.", roleName);
            }
        }

        logger.info("Role initialization complete.");
        // if (userRepository.findByUsername("admin").isEmpty()) {
        //     User admin = new User(...);
        //     admin.setRole(roleRepository.findByName(RoleName.ADMIN).orElseThrow());
        //     userRepository.save(admin);
        //     logger.info("Created default admin user.");
        // }
    }
}