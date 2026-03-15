package com.oneshop.service; // Or your service package

import com.oneshop.entity.Address;
import com.oneshop.entity.User;
import com.oneshop.repository.AddressRepository;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AddressService {

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserService userService; // To find user by username

    
    public List<Address> findByUsernameOrdered(String username) {
        User user = userService.findByUsername(username);
        if (user != null) {
            return addressRepository.findByUserIdOrderByIsDefaultDesc(user.getId());
        }
        return List.of(); // Return empty list if user not found
    }

   
    @Transactional
    public Address save(Address address, String username) {
        
        // 1. Tìm đối tượng User và kiểm tra null
        User user = userService.findByUsername(username);
        if (user == null) {
            throw new EntityNotFoundException("User not found with username: " + username);
        }
        
        // 2. Gán đối tượng User cho Address
        address.setUser(user);

        
        long existingAddressCount = addressRepository.countByUserId(user.getId()); 

        if (existingAddressCount == 0) {
            // Nếu đây là địa chỉ đầu tiên, bắt buộc nó là mặc định
            address.setIsDefault(true);
        } else {
            // Nếu không phải địa chỉ đầu tiên
            if (address.getIsDefault() != null && address.getIsDefault()) {
                // Nếu người dùng chủ động SET CÁI MỚI này là default
                // -> Bỏ default của tất cả các địa chỉ CŨ
                addressRepository.setNonDefaultByUserId(user.getId());
            } else {
                // Nếu người dùng không set (hoặc set là false)
                address.setIsDefault(false);
            }
        }
       
        // 4. Lưu Address vào database
        return addressRepository.save(address);
    }

    @Transactional // Đừng quên @Transactional
    public Address update(Long id, Address addressData, String username) {
        // 1. Tìm user và địa chỉ cũ
        User user = userService.findByUsername(username);
        if (user == null) {
            throw new EntityNotFoundException("User not found: " + username);
        }

        Address existing = addressRepository.findById(id)
            .filter(a -> a.getUser().getId().equals(user.getId())) // Đảm bảo địa chỉ này thuộc về user
            .orElseThrow(() -> new EntityNotFoundException("Address not found with id: " + id));

        // 2. Cập nhật thông tin cơ bản
        existing.setFullName(addressData.getFullName());
        existing.setPhone(addressData.getPhone());
        existing.setAddress(addressData.getAddress());

        Boolean requestedIsDefault = addressData.getIsDefault();

        // 3. Xử lý logic Mặc định (Quan trọng)
        if (requestedIsDefault != null && requestedIsDefault) {
            // Nếu user muốn SET địa chỉ NÀY làm mặc định
            // -> Tắt mặc định của tất cả địa chỉ KHÁC
            addressRepository.setNonDefaultByUserId(user.getId());
            existing.setIsDefault(true);
        } else {
            // Nếu user muốn BỎ SET mặc định của địa chỉ này
            existing.setIsDefault(false);
            
            // Kiểm tra xem đây có phải là địa chỉ mặc định cuối cùng không
            long totalAddresses = addressRepository.countByUserId(user.getId());
            if (totalAddresses > 1) {
                // Đếm xem có cái nào khác là default không
                long defaultCount = addressRepository.countByUserIdAndIsDefaultTrueAndAddressIdNot(user.getId(), id);
                if (defaultCount == 0) {
                    // Nếu không còn cái nào default -> Phải set một cái khác làm default
                    // (ví dụ: cái đầu tiên tìm thấy mà không phải cái đang sửa)
                    Address newDefault = addressRepository.findFirstByUserIdAndAddressIdNot(user.getId(), id)
                                            .orElse(null);
                    if (newDefault != null) {
                        newDefault.setIsDefault(true);
                        addressRepository.save(newDefault);
                    }
                }
            } else if (totalAddresses == 1) {
                 // Nếu đây là địa chỉ duy nhất, BẮT BUỘC nó phải là default
                existing.setIsDefault(true);
            }
        }

        // 4. Lưu địa chỉ đã cập nhật
        return addressRepository.save(existing);
    }
    

   
    public Address setDefaultAddress(Long addressId, String username) {
        User user = userService.findByUsername(username);
        if (user != null) {
            Optional<Address> addressOpt = addressRepository.findById(addressId);
            return addressOpt.filter(a -> a.getUser().getId().equals(user.getId())).map(a -> {
                // Unset all other defaults
                addressRepository.findByUserIdOrderByIsDefaultDesc(user.getId()).forEach(other -> {
                    if (!other.getAddressId().equals(addressId)) {
                        other.setIsDefault(false);
                        addressRepository.save(other);
                    }
                });
                a.setIsDefault(true);
                return addressRepository.save(a);
            }).orElse(null);
        }
        return null; // Return null if user not found or address not found/mismatch
    }
}