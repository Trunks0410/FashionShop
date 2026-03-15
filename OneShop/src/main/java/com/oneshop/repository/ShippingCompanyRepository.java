package com.oneshop.repository;

import com.oneshop.entity.ShippingCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ShippingCompanyRepository extends JpaRepository<ShippingCompany, Long> {
	List<ShippingCompany> findByIsActiveTrueOrderByNameAsc();
}