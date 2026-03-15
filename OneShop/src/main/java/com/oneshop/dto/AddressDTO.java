package com.oneshop.dto;


import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AddressDTO {

    private Long addressId;
    private String fullName;
    private String phone;
    private String address;
    private Boolean isDefault;

    // Constructor from Address entity
    public AddressDTO(com.oneshop.entity.Address address) {
        this.addressId = address.getAddressId();
        this.fullName = address.getFullName();
        this.phone = address.getPhone();
        this.address = address.getAddress();
        this.isDefault = address.getIsDefault();
    }

    // Convert DTO back to Address entity
    public com.oneshop.entity.Address toEntity() {
        com.oneshop.entity.Address address = new com.oneshop.entity.Address();
        address.setAddressId(this.addressId);
        address.setFullName(this.fullName);
        address.setPhone(this.phone);
        address.setAddress(this.address);
        address.setIsDefault(this.isDefault != null ? this.isDefault : false);
        return address;
    }
}