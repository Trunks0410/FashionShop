package com.oneshop.dto;

public class PlaceOrderRequest {
    private Long selectedAddressId;
    private String paymentMethod;
    private String variantIds;

    // Getters and Setters
    public Long getSelectedAddressId() { return selectedAddressId; }
    public void setSelectedAddressId(Long selectedAddressId) { this.selectedAddressId = selectedAddressId; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getVariantIds() { return variantIds; }
    public void setVariantIds(String variantIds) { this.variantIds = variantIds; }
}
