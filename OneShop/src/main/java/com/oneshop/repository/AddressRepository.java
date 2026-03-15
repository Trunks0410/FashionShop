package com.oneshop.repository;

import com.oneshop.entity.Address;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
	List<Address> findByUserIdOrderByIsDefaultDesc(Long userId);

	long countByUserId(Long userId);

	@Transactional
    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.user.id = :userId AND a.isDefault = true")
    void setNonDefaultByUserId(@Param("userId") Long userId);
	@Query("SELECT COUNT(a) FROM Address a WHERE a.user.id = :userId AND a.isDefault = true AND a.addressId != :addressId")
    long countByUserIdAndIsDefaultTrueAndAddressIdNot(@Param("userId") Long userId, @Param("addressId") Long addressId);

    // Tìm một địa chỉ bất kỳ, TRỪ cái có ID này ra
    Optional<Address> findFirstByUserIdAndAddressIdNot(Long userId, Long addressId);
	
}