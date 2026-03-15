//package com.oneshop.service.admin;
//
//import java.util.List;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import com.oneshop.entity.Role;
//import com.oneshop.entity.User;
//import com.oneshop.repository.RoleRepository;
//import com.oneshop.repository.UserRepository;
//
//@Service
//
//public class UserServiceImpl implements UserService{
//
//	private final UserRepository userRepository;
//    private final RoleRepository roleRepository; // Cần inject RoleRepository
//
//    @Autowired
//    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository) {
//        this.userRepository = userRepository;
//        this.roleRepository = roleRepository;
//    }
//
//    @Override
//    public List<User> getAllUsers() {
//        // Lấy tất cả người dùng từ database
//        return userRepository.findAll();
//    }
//
//    @Override
//    public List<User> searchUsers(String keyword) {
//        return userRepository.findByUsernameOrEmailContaining(keyword);
//    }
//
//    @Override
//    @Transactional
//    public void updateUserRole(Long userId, String newRoleName) { 
//        User user = userRepository.findById(userId)
//                                  .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
//
//        Role newRole = roleRepository.findByName(Role.RoleName.valueOf(newRoleName))
//                                     .orElseThrow(() -> new RuntimeException("Role not found with name: " + newRoleName));
//
//        Set<Role> updatedRoles = user.getRoles().stream()
//                .filter(r -> !r.getName().toString().equals("ADMIN") && 
//                             !r.getName().toString().equals("USER"))
//                .collect(Collectors.toSet());
//        
//        updatedRoles.add(newRole);
//        user.setRoles(updatedRoles);
//        userRepository.save(user); 
//    }
//
//    @Override
//    @Transactional
//    public void toggleUserStatus(Long userId) { // Đổi kiểu ID sang Long
//        // 1. Tìm User
//        User user = userRepository.findById(userId)
//                                  .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
//        
//        // 2. Đảo ngược trạng thái 'activated'
//        user.setActivated(!user.isActivated());
//        
//        // 3. Lưu lại
//        userRepository.save(user);
//    }
//
//	@Override
//	public void updateUserRole(Integer userId, String newRole) {
//		// TODO Auto-generated method stub
//		
//	}
//
//	@Override
//	public void toggleUserStatus(Integer userId) {
//		// TODO Auto-generated method stub
//		
//	}
//
//}
