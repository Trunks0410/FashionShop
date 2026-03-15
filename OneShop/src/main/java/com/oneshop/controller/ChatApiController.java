// src/main/java/com/oneshop/controller/ChatApiController.java
package com.oneshop.controller;

import com.oneshop.entity.Role.RoleName;
import com.oneshop.entity.User;
import com.oneshop.repository.ChatMessageRepository;
import com.oneshop.repository.RoleRepository; // Assuming you need this if not directly on User
import com.oneshop.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
public class ChatApiController {

    @Autowired private UserRepository userRepository;
    @Autowired private ChatMessageRepository chatMessageRepository;
    // @Autowired private RoleRepository roleRepository; // Inject RoleRepository if needed

    // *** THÊM role vào DTO ***
    public record ChatUserDto(Long id, String username, String fullName, String email, String role) {}

    @GetMapping("/users")
    public List<ChatUserDto> getChatUsers(Authentication authentication) {
        String currentUsername = authentication.getName();

        List<String> chatParticipants = chatMessageRepository.findChatParticipants(currentUsername);

        List<String> targetUsernames = chatParticipants.stream()
            .filter(name -> name != null && !name.equals(currentUsername))
            .distinct() // Đảm bảo username không lặp lại
            .toList();

        if (targetUsernames.isEmpty()) {
            return List.of(); // Trả về danh sách rỗng nếu không có ai chat
        }

        // Lấy thông tin User entities
        List<User> targetUsers = userRepository.findByUsernameIn(targetUsernames); // Giả sử có hàm findByUsernameIn

        return targetUsers.stream()
                .filter(User::isActivated) // Chỉ lấy user đang hoạt động
                .map(u -> {
                    // *** Xác định vai trò để hiển thị ***
                    String displayRole = "Khách"; // Mặc định là Khách
                    if (u.getRole() != null) {
                        if (u.getRole().getName() == RoleName.SHIPPER) {
                            displayRole = "Shipper";
                        }
                        // Bạn có thể thêm các vai trò khác nếu cần
                    }
                    return new ChatUserDto(
                        u.getId(),
                        u.getUsername(),
                        u.getFullName(),
                        u.getEmail(),
                        displayRole // *** Trả về vai trò ***
                    );
                })
                .toList();
    }
}