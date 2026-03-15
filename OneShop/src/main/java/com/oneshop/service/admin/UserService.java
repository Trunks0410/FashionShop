package com.oneshop.service.admin;
import java.util.List;

import com.oneshop.entity.User;
public interface UserService {
	List<User> getAllUsers();
    List<User> searchUsers(String keyword);
    void updateUserRole(Integer userId, String newRole);
    void toggleUserStatus(Integer userId);
	void updateUserRole(Long userId, String newRoleName);
	void toggleUserStatus(Long userId);
}
