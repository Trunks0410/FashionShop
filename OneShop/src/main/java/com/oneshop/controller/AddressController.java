package com.oneshop.controller;

import com.oneshop.dto.AddressDTO;
import com.oneshop.entity.Address;
import com.oneshop.service.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    @Autowired
    private AddressService addressService;

    @GetMapping("/user")
    public ResponseEntity<List<AddressDTO>> getUserAddresses(Authentication authentication) {
        String username = authentication.getName();
        List<Address> addresses = addressService.findByUsernameOrdered(username);
        List<AddressDTO> addressDTOs = addresses.stream().map(AddressDTO::new).collect(Collectors.toList());
        return ResponseEntity.ok(addressDTOs);
    }

    @PostMapping
    public ResponseEntity<?> createAddress(@Valid @RequestBody AddressDTO addressDTO, BindingResult result, Authentication authentication) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(result.getAllErrors());
        }
        Address address = addressDTO.toEntity();
        String username = authentication.getName();
        Address savedAddress = addressService.save(address, username);
        return ResponseEntity.ok(new AddressDTO(savedAddress));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Address> updateAddress(@PathVariable Long id, 
                                                 @RequestBody Address address, 
                                                 Principal principal) { // <-- 1. Thêm Principal vào tham số
        
        if (principal == null) {
            // Chặn trường hợp người dùng chưa đăng nhập (dù Spring Security sẽ làm việc này)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 2. Lấy username từ Principal
        String username = principal.getName(); 

        // 3. Gọi hàm update với 3 tham số
        try {
            Address updatedAddress = addressService.update(id, address, username);
            return ResponseEntity.ok(updatedAddress);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }
}