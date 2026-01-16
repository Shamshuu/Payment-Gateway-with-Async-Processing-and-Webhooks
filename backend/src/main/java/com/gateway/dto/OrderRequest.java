package com.gateway.dto;

public class OrderRequest {
    
    private Long amount;
    private String currency;
    private String receipt;

    // Getters and Setters
    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getReceipt() { return receipt; }
    public void setReceipt(String receipt) { this.receipt = receipt; }
}