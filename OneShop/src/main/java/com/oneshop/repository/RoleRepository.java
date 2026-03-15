// src/main/java/com/oneshop/repository/RoleRepository.java
package com.oneshop.repository; // Đảm bảo đúng package

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.oneshop.entity.Role; // Import entity Role mới
import com.oneshop.entity.Role.RoleName; // Import Enum RoleName

// Không cần @Repository vì JpaRepository đã đủ
public interface RoleRepository extends JpaRepository<Role, Long> {

    // Phương thức tìm Role bằng RoleName (Enum)
    Optional<Role> findByName(RoleName name);
}