package com.oneshop.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "shipping_rules") 
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShippingRule {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long ruleId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "shipping_company_id", nullable = false)
	private ShippingCompany company; 

	@Column(length = 50) 
	private String ruleName;
	
	@Column(length = 50) 
	private String fromRegion;

	@Column(length = 50) 
	private String toRegion;
	
	@Column(nullable = false)
	private Boolean isExpress = false; 
	
	@Column(name = "estimated_delivery_time", length = 50) 
	private String estimatedDeliveryTime;

	@Column(precision = 10, scale = 2, nullable = false)
	private BigDecimal baseFee; 
}