package com.oneshop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.oneshop.entity.ShippingRule;
@Repository
public interface ShippingRuleRepository extends JpaRepository<ShippingRule, Long>{

}
